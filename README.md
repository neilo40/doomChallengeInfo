# The Hackathon Doom challenge

We want you to build a bot to play Doom using a REST interface.  We will be putting on two challenges and we want you to enter both, so think about how you will split your time and resources.

### Deathmatch
This level will be a simple "bowl" containing weapons, three other players and very little in the way of obstructions.  Your bot will need to figure out where the weapons are, where the other players are and the best strategy for emerging victorious

### Single Player
We want you to write a bot that can successfully complete at least E1M1 of the shareware wad (See below).  This will require a lot more thought and energy.  How will you decide where the bot can go?  How can you generate a path from where you are to where you want to go?  QuadTrees, graph, Dijkstra and A* will all come in useful here... 

## How to get the engine
The source is on github and instructions to build it on your local machine are below.  If you get stuck, please just ask for help.  For best results, please use native hardware if possible.  The engine does not run very well in a VM.

If you encounter any bugs, or have feature requests please report them (or better yet, raise a PR!).  We will be available to tweak the engine throughout the weekend

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
brew install sdl2_mixer 
brew install sdl2_net
git clone https://github.com/neilo40/restful-doom.git
cd restful-doom
aclocal -I /usr/local/share/aclocal
autoheader
automake -a -c
autoconf -I/usr/local/share/aclocal
./configure --with-sdl-prefix=/usr/local
make
```

### Windows 10
We got the best results using the WSL (Windows Subsystem for Linux)
1. Install ubuntu through WSL (available in the store)
2. Start a bash shell
```bash
sudo apt install libsdl2-2.0-0 libsdl2-dev libsdl2-mixer-2.0-0 libsdl2-mixer-dev libsdl2-net-2.0-0 libsdl2-net-dev
sudo apt install autotools-dev autoconf automake gcc make pkg-config
sudo apt install git
git clone https://github.com/neilo40/restful-doom.git
cd restful-doom
./configure-and-build.sh
```
3. Download, install and run [VcXsrv](https://sourceforge.net/projects/vcxsrv/) (or X server of your choice)

## How to run the engine
During development, you may run the game standalone (i.e. non-server) mode, or you may start a server to test your bot against itself (or others).  During development you will have access to the full API, including restricted endpoints.  During the judging you will not be able to use these.

Config files will be placed into ~/.restful-doom/ which you can use to configure your client.  e.g. disable mouse, set screen size, player name etc

### Notes
- *On Windows you will need to export DISPLAY=:0*
- *On MacOS you will need to pass the -nosound argument or SDL will segfault*

### Single player
In this mode, all API endpoints are available to use
```bash
restful-doom -iwad ~/Downloads/doom1.wad -apiport 6001
```

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
To use a custom wad, e.g. our Arena.wad, you will need to provide the non-shareware wad as the iwad arg, then the Arena wad as the -file arg:

```bash
restful-doom -iwad doom.wad -file Arena.wad ...
```

## How to interact with the game world
All interaction will be via REST api, described [here](http://htmlpreview.github.io/?https://github.com/neilo40/restful-doom/blob/master/RAML/doom_api.html) 

You will be able to query the game world for locations and states of all game objects (monsters, objects, players, doors).  You will also be able to issue commands to your player to move, turn, shoot, change weapon etc.

During development you will also be able to do things such as spawn objects, give your player health, weapons, armor etc.  During judging, you will not be able to access these.

## WAD files
Wad is the format that id used for storing all of the information about the game levels.  You will want to parse the wad files we provide so that you can start to build a model of the level with which to control your bot.

We have provided the shareware wad, and a custom designed deathmatch level.  See the Wads folder to download.

### Sample code
We have some sample code which you can use to get started with parsing the  wad file.  See WadParser.scala in this project, or https://gist.github.com/jasonsperske/42284303cf6a7ef19dc3 for a python version.

### Links
For some documentation on the format of the WAD file, have a look at:
- http://doom.wikia.com/wiki/WAD
- exploded diagram TBC
