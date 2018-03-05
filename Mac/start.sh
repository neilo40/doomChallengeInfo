#!/bin/bash
DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
DYLD_LIBRARY_PATH=$DIR $DIR/restful-doom -iwad $DIR/../doom1.wad -apiport 6001
