# Weather Front

A WebGL ClojureScript front visualizing climate change models.


# Setup

## Leiningen

First install [leiningen](http://leiningen.org/), the popular clojure
build system. On Ubuntu/Debian, run:

    sudo apt install leiningen

On macOS with [homebrew](http://brew.sh/):

    brew install leiningen

On Windows there's apparently an [installer](http://leiningen-win-installer.djpowell.net/).


## Our code

With that installed, in order to get an interactive development
environment run:

    lein figwheel

and open your browser at [localhost:3449](http://localhost:3449/).
This will auto compile and send all changes to the browser without the
need to reload. After the compilation process is complete, you will
get a Browser Connected REPL. An easy way to try it is:

    (js/alert "Am I connected?")

and you should see an alert in the browser window.

To clean all compiled files:

    lein clean

To create a production build run:

    lein do clean, cljsbuild once min

And open your browser in `resources/public/index.html`. You will not
get live reloading, nor a REPL.

# License

Copyright © 2016 WeatherMagic, Licensed under AGPLv3.
