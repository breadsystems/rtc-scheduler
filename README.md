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

To build the dev environment, run:

```sh
clj -A:dev -m rtc.app
```