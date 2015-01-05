#!/bin/bash

# Copyright (c) 2014, CableLabs, Inc.
# All rights reserved.
#
# Redistribution and use in source and binary forms, with or without
# modification, are permitted provided that the following conditions are met:
#
# 1. Redistributions of source code must retain the above copyright notice,
# this list of conditions and the following disclaimer.
#
# 2. Redistributions in binary form must reproduce the above copyright notice,
# this list of conditions and the following disclaimer in the documentation
# and/or other materials provided with the distribution.
#
# THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
# AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
# IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
# ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
# LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
# CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
# SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
# INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
# CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
# ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
# POSSIBILITY OF SUCH DAMAGE.

avconv=avconv

function usage {
  echo ""
  echo "Transcode/Transrate Script"
  echo "usage:"
  echo "   video_scaled_demux_5bitrates <input_file> <output_dir>"
}

if [ -z $1 ]; then
  echo "Must provide input media file"
  usage
  exit 1
fi
if [ -z $2 ]; then
  echo "Must provide output directory for transcoded/transrated files"
  usage
  exit 1
fi

mkdir -p $2

$avconv -threads auto -i $1 -vf "scale=w=512:h=288" -map_chapters -1 -an -codec:v libx264 -profile:v main -level 21 -b:v 360k $2/video_512x288_h264-360Kb.mp4

$avconv -threads auto -i $1 -vf "scale=w=704:h=396" -map_chapters -1 -an -codec:v libx264 -profile:v main -level 30 -b:v 620k $2/video_704x396_h264-620Kb.mp4

$avconv -threads auto -i $1 -vf "scale=w=896:h=504" -map_chapters -1 -an -codec:v libx264 -profile:v high -level 31 -b:v 1340k $2/video_896x504_h264-1340Kb.mp4

$avconv -threads auto -i $1 -vf "scale=w=1280:h=720" -map_chapters -1 -an -codec:v libx264 -profile:v high -level 32 -b:v 2500k $2/video_1280x720_h264-2500Kb.mp4

$avconv -threads auto -i $1 -vf "scale=w=1920:h=1080" -map_chapters -1 -sn -an -codec:v libx264 -profile:v high -level 40 -b:v 4500k  $2/video_1920x1080_h264-4500Kb.mp4

$avconv -threads auto -i $1 -map_chapters -1 -vn -codec:a libfdk_aac -profile:a aac_low -b:a 128k $2/audio_aac-lc_128k.mp4

$avconv -threads auto -i $1 -map_chapters -1 -vn -codec:a libfdk_aac -profile:a aac_low -b:a 192k $2/audio_aac-lc_192k.mp4

