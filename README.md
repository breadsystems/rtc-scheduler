# RTC Care Schedule

> Unbuilding the medical industrial complex one appointment at a time

Calendar and scheduling system for the Radical Telehealth Collective

## About this app

TODO

### Features

* Frontend for people seeking medical care to book an appointment and enter their info
* Scheduling application for medical providers and volunteers to communicate with care-seekers and coordinate access needs such as interpreters, captioners, etc.

## Running the means of production

This application ships a Docker image, which is the recommended method of running in production.

```sh
docker run -it -p 8080:8080 radtelehealth/rtc-calendar
```

## Building the means of production

First build the uberjar. This is basically an executable archive containing all the files you need to run in production (sans runtime dependencies, such as a database).

```sh
deploy/package.sh
```

Then, build the Docker image:

```sh
docker build . -t rtc:latest
```

## Building the movement

To build the dev environment, you will first need to install [Lando](https://docs.lando.dev/), the official dev environment for RTC. It's the best local dev tool in the galaxy! Note that Lando runs on top of Docker, but it will install a recent version of Docker for you if you don't already have it.

Next, get the RTC source code:

```sh
# TODO figure out where we're hosting this code
cd radical-telehealth-collective
```

Once Lando is installed, you can start the dev environment with a single command:

```sh
lando start 
```

This will initialize the database and start the dev environment. This may take a few minutes the first time, so be patient. When it's done, it will print the URL of the local RTC app to your terminal screen.

### TODO

* Link to Clojure guides
* Document basic app architecture
