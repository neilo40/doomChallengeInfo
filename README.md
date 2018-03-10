# The Hackathon Doom challenge

We want you to build a bot to play Doom using a REST interface against other teams. 

### Deathmatch
This level will be a simple "bowl" containing weapons, three other players and very little in the way of obstructions.  Your bot will need to figure out where the weapons are, where the other players are and the best strategy for emerging victorious

## How to get the engine
Use the binaries compiled for Linux, Mac and Windows in this project.  If you need to compile from source, see us, or follow the instructions below

### Linux
```bash
sudo apt install libxext-dev m4 automake
git clone https://github.com/neilo40/restful-doom.git
cd restful-doom
./configure-and-build.sh
```

### MacOS
1. Install homebrew (https://brew.sh/)
```bash
brew install autoconf
brew install automake
brew install pkg-config
brew install sdl2
git clone https://github.com/neilo40/restful-doom.git
cd restful-doom
aclocal -I /usr/local/share/aclocal
autoheader
mkdir autotools (may not be required)
automake -a -c
autoconf -I/usr/local/share/aclocal
./configure-and-build.sh
```

### Windows 10
See us

## How to run the engine
During development, you may start a server to test your bot against itself (or others).  During development you will have access to the full API, including restricted endpoints.  During the judging you will not be able to use these.

Config files will be placed into ~/.restful-doom/ which you can use to configure your client.  e.g. disable mouse, set screen size, player name etc

### Notes
- *On MacOS you will need to pass the -nosound argument or SDL will segfault*

### Multi player
In this mode, only the player hosting the server may use restricted endpoints

```bash
restful-doom -apiport 6001 -iwad ~/Downloads/doom1.wad -server -deathmatch -privateserver \
    -nomonsters -nosound&
restful-doom -apiport 6002 -iwad ~/Downloads/doom1.wad -connect localhost \
    -extraconfig ~/.restful-doom/restful-doom_p2.cfg&
restful-doom -apiport 6003 -iwad ~/Downloads/doom1.wad -connect localhost \
    -extraconfig ~/.restful-doom/restful-doom_p3.cfg&
restful-doom -apiport 6004 -iwad ~/Downloads/doom1.wad -connect localhost \
    -extraconfig ~/.restful-doom/restful-doom_p4.cfg&
```

### Dedicated server
We will use this mode during judging.  No player will be able to access restricted API endpoints

```bash
restful-doom -privateserver -dedicated&

# First client sets the game params
restful-doom -apiport 6001 -iwad ~/Downloads/doom1.wad -connect localhost \
    -extraconfig ~/.restful-doom/restful-doom_p1.cfg -deathmatch -nomonsters -noaudio&
sleep 1 # ensure 6001 is the first player

# start other clients
restful-doom -apiport 6002 -iwad ~/Downloads/doom1.wad -connect localhost \
    -extraconfig ~/.restful-doom/restful-doom_p2.cfg&
restful-doom -apiport 6003 -iwad ~/Downloads/doom1.wad -connect localhost \
    -extraconfig ~/.restful-doom/restful-doom_p3.cfg&
restful-doom -apiport 6004 -iwad ~/Downloads/doom.wad -connect localhost \
    -extraconfig ~/.restful-doom/restful-doom_p4.cfg&
```

### To use a custom wad
To use a custom wad, e.g. our Arena.wad, you will need to provide the non-shareware wad (doom.wad, not doom1.wad) as the iwad arg, then the Arena wad as the -merge arg:

```bash
restful-doom -iwad doom.wad -merge Arena.wad ...
```

## How to interact with the game world
All interaction will be via REST api, described [here](http://htmlpreview.github.io/?https://github.com/neilo40/restful-doom/blob/master/RAML/doom_api.html) 

You will be able to query the game world for locations and states of all game objects (monsters, objects, players, doors).  You will also be able to issue commands to your player to move, turn, shoot, change weapon etc.

During development you will also be able to do things such as spawn objects, give your player health, weapons, armor etc.  During judging, you will not be able to access these.

