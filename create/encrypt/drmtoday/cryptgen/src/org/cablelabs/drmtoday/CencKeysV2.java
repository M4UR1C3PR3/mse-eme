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

package org.cablelabs.drmtoday;

/**
 * JSON object that serves as message body for DRMToday CommonEncryption key
 * ingest API V2
 */
public class CencKeysV2 {
    
    public class IngestKey {
        public String streamType;              // One of "UHD", "HD", "SD", "AUDIO", "VIDEO", "VIDEO_AUDIO"
        public int keyRotationId;              // Optional
        public String keyId;                   // Base64-encoded key ID (16 bytes)
        public String key;                     // Base64-encoded key (16 bytes)
        public String algorithm = "AES";
        public String iv;                      // Base64-encoded initialization vector (16 bytes)
        public String wvAssetId;               // Optional
    }
    
    public class Asset {
        public String type = "CENC";       
        public String assetId;                  // Required
        public String variantId;                // Optional
        public boolean overwriteExistingKeys = true;
        public String keySeedId;                // UUID String, must be set if not specifying keys in "assets"
        public String ivSeedId;                 // UUID String, don't set if using "iv" in "assets"    
        public IngestKey[] ingestKeys;
    }

    public Asset[] assets;
}
