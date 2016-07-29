/* Copyright (c) 2015, CableLabs, Inc.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

package org.cablelabs.drmtoday.cryptgen;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.binary.Hex;
import org.cablelabs.primetime.cryptfile.PrimetimePSSH;
import org.cablelabs.clearkey.cryptfile.ClearKeyPSSH;
import org.cablelabs.cmdline.CmdLine;
import org.cablelabs.cryptfile.CryptKey;
import org.cablelabs.cryptfile.CryptTrack;
import org.cablelabs.cryptfile.CryptfileBuilder;
import org.cablelabs.cryptfile.DRMInfoPSSH;
import org.cablelabs.cryptfile.KeyPair;
import org.cablelabs.drmtoday.AuthAPI;
import org.cablelabs.drmtoday.CencKeyAPI2;
import org.cablelabs.drmtoday.CencKeysV2;
import org.cablelabs.drmtoday.PropsFile;
import org.cablelabs.drmtoday.PsshData;
import org.cablelabs.drmtoday.cryptfile.DRMTodayPSSH;
import org.cablelabs.playready.cryptfile.PlayReadyPSSH;
import org.cablelabs.widevine.cryptfile.WidevinePSSH;
import org.w3c.dom.Document;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * This utility will build a MP4Box cryptfile for a given piece of content using DRMToday.  The steps
 * involved are:
 * <ol>
 *   <li>Generate randomg keys/keyIDs for each track</li>
 *   <li>Ingest the created keys into the CommonEncryption keystore for the given DRMToday account</li>
 *   <li>Generate the PSSH boxes for each desired DRM as returned by the DRMToday key ingest</li>
 *   <li>Generate the MP4Box cryptfile</li>
 * </ol>
 *
 * The 
 */
public class CryptfileGen {
    
    private static class Usage implements org.cablelabs.cmdline.Usage {
        public void usage() {
            System.out.println("DRMToday MP4Box cryptfile generation tool.");
            System.out.println("");
            System.out.println("usage:  CryptfileGen [OPTIONS] <drmtoday_props_file> <assetId>");
            System.out.println("\t\t <track_id>:<track_type>[:{@<keyid_file>|<key_id>=<key>[,<key_id>=<key>...]}]");
            System.out.println("\t\t [<track_id>:<track_type>[:{@<keyid_file>|<key_id>=<key>[,<key_id>=<key>...]}]]...");
            System.out.println("");
            System.out.println("\t<drmtoday_props_file>");
            System.out.println("\t\tDRMToday properties file that contains merchant login info.  <drmtoday_props_file>");
            System.out.println("\t\tis a Java properties file with the following properties:");
            System.out.println("\t\t\tmerchant: Your assigned merchant ID");
            System.out.println("\t\t\tusername: Your DRMToday frontend username");
            System.out.println("\t\t\tpassword: Your DRMToday frontend password");
            System.out.println("\t\t\tauthHost: Host to use for DRMToday CAS operations");
            System.out.println("\t\t\tfeHost: Host to use for DRMToday frontend operations");
            System.out.println("");
            System.out.println("\t<assetId> The DRMToday assetId");
            System.out.println("");
            System.out.println("\t<track_id> is the track ID from the MP4 file to be encrypted");
            System.out.println("");
            System.out.println("\t<track_type> is one of AUDIO, VIDEO, or VIDEO_AUDIO describing the content type of the");
            System.out.println("\tassociated track");
            System.out.println("");
            System.out.println("\tOPTIONS:");
            System.out.println("");
            System.out.println("\t-help");
            System.out.println("\t\tDisplay this usage message.");
            System.out.println("");
            System.out.println("\t-out <filename>");
            System.out.println("\t\tIf present, the cryptfile will be written to the given file. Otherwise output will be");
            System.out.println("\t\twritten to stdout");
            System.out.println("");
            System.out.println("\t-variantId");
            System.out.println("\t\tOptional DRMToday asset variantId.");
            System.out.println("");
            System.out.println("\t-roll <num_keys>,<num_samples>");
            System.out.println("\t\tUse rolling keys.  <num_keys> unique encryption keys will be used to encrypt the samples.");
            System.out.println("\t\t<num_samples> samples will be encrypted with each key before moving to the next key in the");
            System.out.println("\t\tlist of keys.  If the key/keyID pairs were specified on the command line, they must match");
            System.out.println("\t\t<num_keys> or an error will be reported.");
            System.out.println("");
            System.out.println("\t-ck");
            System.out.println("\t\tAdd ClearKey PSSH to the cryptfile.");
            System.out.println("");
            System.out.println("\t-wv");
            System.out.println("\t\tAdd Widevine PSSH to the cryptfile.");
            System.out.println("");
            System.out.println("\t-pr");
            System.out.println("\t\tAdd PlayReady PSSH to the cryptfile.");
            System.out.println("");
            System.out.println("\t-pt");
            System.out.println("\t\tAdd Primetime PSSH to the cryptfile.");
            System.out.println("");
            System.out.println("\t-cp");
            System.out.println("\t\tPrint a DASH <ContentProtection> element (for each DRM) that can be pasted into the MPD");
        }
    }
    
    private enum StreamType {
        UHD,
        HD,
        SD,
        AUDIO,
        VIDEO,
        VIDEO_AUDIO,
        NUM_TYPES
    }
    
    private static class Track {
        int id;
        List<KeyPair> keypairs;
        StreamType streamType;
    }
    
    // Returns null if new track type would be valid, otherwise, if it would conflict with existing
    // track types, returns the conflicting type
    private static String validateTrackStreamType(Track[] trackList, StreamType newTrack) {
        List<StreamType> conflictingTypes = new ArrayList<StreamType>();
        switch (newTrack) {
            case UHD:
            case HD:
            case SD:
                conflictingTypes.add(StreamType.VIDEO);
                conflictingTypes.add(StreamType.VIDEO_AUDIO);
                break;
            case AUDIO:
                conflictingTypes.add(StreamType.VIDEO_AUDIO);
                break;
            case VIDEO:
                conflictingTypes.add(StreamType.UHD);
                conflictingTypes.add(StreamType.HD);
                conflictingTypes.add(StreamType.SD);
                conflictingTypes.add(StreamType.VIDEO_AUDIO);
                break;
            case VIDEO_AUDIO:
                conflictingTypes.add(StreamType.UHD);
                conflictingTypes.add(StreamType.HD);
                conflictingTypes.add(StreamType.SD);
                conflictingTypes.add(StreamType.AUDIO);
                conflictingTypes.add(StreamType.VIDEO);
                break;
        }
        for (StreamType st : conflictingTypes) {
            if (trackList[st.ordinal()] != null)
                return st.toString();
        }
        return null;
    }
    
    public static void main(String[] args) {
        
        CmdLine cmdline = new CmdLine(new Usage());

        // DRMToday login properties file
        String dtPropsFile = null;
        
        String assetId = null;
        String variantId = null;
        
        String outfile = null;
        
        int numRollingKeys = 0;
        int rollingKeySamples = 0;
        
        // DRMs
        boolean clearkey = false;
        boolean widevine = false;
        boolean playready = false;
        boolean primetime = false;
        
        // Print content protection element?
        boolean printCP = false;
        
        Track[] trackList = new Track[StreamType.NUM_TYPES.ordinal()];

        // Parse arguments
        for (int i = 0; i < args.length; i++) {
            
            // Parse options
            if (args[i].startsWith("-")) {
                String[] subopts;
                if ((subopts = cmdline.checkOption("-help", args, i, 0)) != null ||
                     (subopts = cmdline.checkOption("-h", args, i, 0)) != null) {
                    (new Usage()).usage();
                    System.exit(0);
                }
                else if ((subopts = cmdline.checkOption("-out", args, i, 1)) != null) {
                    outfile = subopts[0];
                    i++;
                }
                else if ((subopts = cmdline.checkOption("-variantId", args, i, 1)) != null) {
                    variantId = subopts[0];
                    i++;
                }
                else if ((subopts = cmdline.checkOption("-ck", args, i, 0)) != null) {
                    clearkey = true;
                }
                else if ((subopts = cmdline.checkOption("-wv", args, i, 0)) != null) {
                    widevine = true;
                }
                else if ((subopts = cmdline.checkOption("-pr", args, i, 0)) != null) {
                    playready = true;
                }
                else if ((subopts = cmdline.checkOption("-pt", args, i, 0)) != null) {
                    primetime = true;
                }
                else if ((subopts = cmdline.checkOption("-cp", args, i, 0)) != null) {
                    printCP = true;
                }
                else if ((subopts = cmdline.checkOption("-roll", args, i, 2)) != null) {
                    numRollingKeys = Integer.parseInt(subopts[0]);
                    rollingKeySamples = Integer.parseInt(subopts[1]);
                    i++;
                }
                else {
                    cmdline.errorExit("Illegal argument: " + args[i]);
                }
                
                continue;
            }
            
            // Get login properties file
            if (dtPropsFile == null) {
                dtPropsFile = args[i];
                continue;
            }
            
            // Get login properties file
            if (assetId == null) {
                assetId = args[i];
                continue;
            }
            
            // Parse tracks
            String track_desc[] = args[i].split(":");
            if (track_desc.length < 2) {
                cmdline.errorExit("Illegal track specification: " + args[i]);
            }
            try {
                Track t = new Track();
                StreamType streamType = StreamType.valueOf(track_desc[1]);
                t.streamType = streamType;
                
                String conflict = validateTrackStreamType(trackList, t.streamType);
                if (conflict != null) {
                    cmdline.errorExit("New track stream type (" + t.streamType.toString() + ") conflicts with previous track (" + conflict + ")");
                }

                t.id = Integer.parseInt(track_desc[0]);

                t.keypairs = new ArrayList<KeyPair>();
                if (track_desc.length == 3) { // Keys specified
                    // Read key IDs from file
                    if (track_desc[1].startsWith("@")) {
                        String keyfile = track_desc[1].substring(1);
                        BufferedReader br = null;
                        try {
                            br = new BufferedReader(new FileReader(keyfile));
                            String line;
                            while ((line = br.readLine()) != null) {
                                String[] key = line.split("=");
                                if (key.length != 2)
                                    throw new IllegalArgumentException("Invalid key specification in key file: " + line);
                                t.keypairs.add(new KeyPair(key[0],key[1]));
                            }
                        } catch (Exception e) {
                            throw new IllegalArgumentException("Error parsing key file! (" + e.getMessage() + ")");
                        } finally {
                            if (br != null)
                                try { br.close(); } catch (IOException e) { }
                        }
                    }
                    else { // Key IDs on command line
                        String[] keys = track_desc[1].split(",");
                        for (String keypair : keys) {
                            String[] key = keypair.split("=");
                            if (key.length != 2) 
                                throw new IllegalArgumentException("Invalid cmdline key specification: " + keypair);
                            t.keypairs.add(new KeyPair(key[0],key[1]));
                        }
                    }
                } else { // Generate random keys
                    int numKeysToGenerate = (rollingKeySamples == 0) ? 1 : numRollingKeys;
                    for (int k = 0; k < numKeysToGenerate; k++) 
                        t.keypairs.add(KeyPair.random()); // Create a random key pair
                }
                trackList[t.streamType.ordinal()] = t;
            }
            catch (IllegalArgumentException e) {
                cmdline.errorExit("Illegal track_type -- " + track_desc[1] + ". " + e.getMessage());
            }
        }
        
        if (dtPropsFile == null) {
            cmdline.errorExit("Must specify login props file!");
        }
        
        if (assetId == null) {
            cmdline.errorExit("Must specify assetId!");
        }
        
        // Load properties file
        PropsFile props = null;
        try {
            props = new PropsFile(dtPropsFile);
        } catch (Exception e) {
            cmdline.errorExit("Error loading DRMToday properties file! -- " + e.getMessage());
        }
        
        // Must use one non-ClearKey DRM
        if (!widevine && !playready) {
            cmdline.errorExit("Must specify at least one non-ClearKey DRM!");
        }
        
        // Login and get ticket for key ingest API
        AuthAPI drmtodayAuth = new AuthAPI(props.getUsername(), props.getPassword(), props.getAuthHost());
        try {
            drmtodayAuth.login();
            
        } catch (Exception e) {
            cmdline.errorExit("Error during DRMToday CAS process! -- " + e.getMessage());
        }
        
        List<DRMInfoPSSH> psshList = new ArrayList<DRMInfoPSSH>();
        List<CryptTrack> cryptTracks = new ArrayList<CryptTrack>();
        
        // Ingest key(s) for each track.  We only do one asset at a time for now
        CencKeysV2 cencKeys = new CencKeysV2();
        cencKeys.assets = new CencKeysV2.Asset[1];
        CencKeysV2.Asset asset = cencKeys.new Asset();
        cencKeys.assets[0] = asset;
        asset.assetId = assetId;
        if (variantId != null) {
            asset.variantId = variantId;
        }
        CencKeyAPI2 cencKeyAPI = new CencKeyAPI2(drmtodayAuth, props.getFeHost(), props.getMerchant());
        for (Track t : trackList) {
            if (t == null)
                continue;

            List<CryptKey> keyList = new ArrayList<CryptKey>();
            int rotationId = (t.keypairs.size() == 1) ? -1 : 1;
            int keyIdx = 0;
            asset.ingestKeys = new CencKeysV2.IngestKey[t.keypairs.size()];
            for (KeyPair kp : t.keypairs) {
                CencKeysV2.IngestKey ingestKey = cencKeys.new IngestKey();
                ingestKey.streamType = t.streamType.toString();
                if (rotationId != -1)
                    ingestKey.keyRotationId = rotationId++;
                ingestKey.keyId = Base64.encodeBase64String(kp.getID());
                ingestKey.key = Base64.encodeBase64String(kp.getKey());
                asset.ingestKeys[keyIdx++] = ingestKey;
            }

            try {
                String resp = cencKeyAPI.ingestKey(cencKeys);
                
                // Parse JSON response
                JsonParser parser = new JsonParser();
                JsonArray respAssets = parser.parse(resp).getAsJsonObject().get("assets").getAsJsonArray();
                if (respAssets.size() != 1)
                    cmdline.errorExit("Expected only one asset in JSON response, but got " + respAssets.size());
                JsonObject respAsset = respAssets.get(0).getAsJsonObject();

                // Validate fields and check for errors
                if (!respAsset.get("assetId").getAsString().equals(assetId))
                    cmdline.errorExit("Response assetId not what was expected: " + respAsset.get("assetId").getAsString());
                if (variantId != null && !respAsset.get("variantId").getAsString().equals(variantId)) 
                    cmdline.errorExit("Response variantId not what was expected: " + respAsset.get("variantId").getAsString());
                if (respAsset.get("errors") != null && respAsset.get("errors").getAsJsonArray().size() != 0) {
                    JsonArray errors = respAsset.get("errors").getAsJsonArray();
                    for (int errIdx = 0; errIdx < errors.size(); errIdx++) {
                        System.out.println("\t " + errors.get(errIdx).getAsString());
                    }
                    cmdline.errorExit("Errors in license ingest response!");
                }
                
                JsonArray respKeys = respAsset.get("keys").getAsJsonArray();
                for (int respKeyIdx = 0; respKeyIdx < respKeys.size(); respKeyIdx++) {
                    JsonObject respKey = respKeys.get(respKeyIdx).getAsJsonObject();
                    byte[] key = Base64.decodeBase64(respKey.get("key").getAsString());
                    byte[] keyID = Base64.decodeBase64(respKey.get("keyId").getAsString());
                    boolean alreadyExisted = respKey.get("alreadyExisted").getAsBoolean();
                    if (alreadyExisted) {
                        System.out.println("WARNING:  KeyID already exists for this asset! " + KeyPair.toGUID(keyID));
                    }
                    
                    List<PsshData> psshdata = PsshData.parseFromDrmTodayJson(respKey.get("cencResponse").toString());
                    for (PsshData d : psshdata) {
                        // Add DRMToday PSSH boxes if requested
                        if ((WidevinePSSH.isWidevine(d.getSystemID()) && widevine) || 
                            (PlayReadyPSSH.isPlayReady(d.getSystemID()) && playready) ||
                            (PrimetimePSSH.isPrimetime(d.getSystemID()) && primetime)) {
                            psshList.add(new DRMTodayPSSH(d));
                        }
                    }
                
                    keyList.add(new CryptKey(new KeyPair(keyID, key)));
                }

            }
            catch (Exception e) {
                // TODO Auto-generated catch block
                System.out.println("Error during Cenc key ingest! -- " + e.getMessage());
            }
            
            cryptTracks.add(new CryptTrack(t.id, 8, null, keyList, rollingKeySamples));
        }
        
        // Add clearkey PSSH if requested
        if (clearkey) {
            int keyCount = 0;
            for (CryptTrack t : cryptTracks) {
                keyCount += t.getKeys().size();
            }
            byte[][] keyIDs = new byte[keyCount][];
            int i = 0;
            System.out.println("Ensure the following keys are available to the client:");
            for (CryptTrack t : cryptTracks) {
                for (CryptKey key : t.getKeys()) {
                    System.out.println("\t" + Hex.encodeHexString(key.getKeyPair().getID()) +
                                       " : " + Hex.encodeHexString(key.getKeyPair().getKey()) +
                                       " (" + Base64.encodeBase64String(key.getKeyPair().getID()) +
                                       " : " + Base64.encodeBase64String(key.getKeyPair().getKey()) + ")");
                    keyIDs[i++] = key.getKeyPair().getID();
                }
            }
            System.out.println("");
            psshList.add(new ClearKeyPSSH(keyIDs));
        }
        
        // Print ContentProtection element
        if (printCP) {
            System.out.println("############# Content Protection Element #############");
            for (DRMInfoPSSH pssh : psshList) {
                Document d = CryptfileBuilder.newDocument();
                try {
                    d.appendChild(pssh.generateContentProtection(d));
                }
                catch (IOException e) {
                    System.out.println("Could not generate ContentProtection element!");
                    continue;
                }
                CryptfileBuilder.writeXML(d, System.out);
            }
            System.out.println("######################################################");
        }
        
        CryptfileBuilder cfBuilder = new CryptfileBuilder(CryptfileBuilder.ProtectionScheme.AES_CTR,
                                                          cryptTracks, psshList);
        
        // Write the output
        Document d = cfBuilder.buildCryptfile();
        CryptfileBuilder.writeXML(d, System.out);
        try {
            if (outfile != null) {
                System.out.println("Writing cryptfile to: " + outfile);
                CryptfileBuilder.writeXML(d, new FileOutputStream(outfile));
            }
        }
        catch (FileNotFoundException e) {
            cmdline.errorExit("Could not open output file (" + outfile + ") for writing");
        }
        
    }
}
