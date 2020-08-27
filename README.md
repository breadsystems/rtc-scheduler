# RTC Care Schedule

> Unbuilding the Medical Industrial Complex one appointment at a time

Calendar and scheduling system for the Radical Telehealth Collective

## About the RTC

When the COVID-19 pandemic began, crip, disabled, and chronically ill communities, especially those of us who are queer, trans, non-binary, BIPOC, immigrants, and/or low-income knew we’d be among those most impacted and that our essential healthcare needs — which the Medical Industrial Complex was already epically failing to meet pre-pandemic — would be even harder to access.

[Health Justice Commons](https://www.healthjusticecommons.org/) took immediate action to ensure our communities’ survival and united to create the Radical Telehealth Collective. The RTC is a collective of rad frontline healthcare providers, rad crips/ disability justice organizers, and Health Justice Commons members uniting to create free, accessible, and multilingual urgent and essential care and access to COVID-19 testing. 

Our virtual clinics are real-world liberated zones. We don’t have “patients” — a term which reinforces the oppressive power structures of the Medical Industrial Complex. Instead, RTC healthcare providers deliver care in the ways they’ve dreamed of but have been prevented by the Medical Industrial Complex: with respect, compassion, humility; honoring the autonomy, wisdom, and dignity of the person with whom they are working. The RTC model is grounded in disability justice and intersectional health justice principles and practices. We center access and offer multilingual interpretation and translation, live closed captioning, and ASL. By providing these services through teleconference, we aim to be able to help connect those members of our communities most impacted by social oppression, environmental racism, healthcare exclusions, or most vulnerable and unable to access care in the traditional models of the Medical Industrial Complex. We’ll learn as we go, allowing the communities we center to lead.


### Features

* Frontend for people seeking medical care to book an appointment and enter their info
* Backend calendar for medical providers to schedule their availability
* Scheduling application for volunteers to communicate with careseekers and coordinate access needs such as interpreters, captioners, etc.

## Running the means of production

This application ships an "uberjar," which is the recommended method of running in production. The build script outputs the uberjar to `target/rtc.jar`, which can then be run just like any other .jar file:

```sh
DATABASE_URL=your-db-url AUTHY_API_KEY=your-api-key \
  java -cp target/rtc.jar clojure.main -m rtc.app # run the jar
```

## Building the means of production

The build script contains everything you need to build the production uberjar. Just run it:

```sh
bin/build
```

This application is hosted on Heroku. To deploy, build the uberjar as describe above and run:

```sh
heroku deploy:jar target/rtc.jar --app radical-telehealth-collective
```

You will need to be logged in to the [Heroku CLI](https://devcenter.heroku.com/articles/heroku-cli) to do this.

## Building the movement

### Manual installation (recommended)

#### Local install prerequisites

* [PostgreSQL](https://www.postgresql.org/docs/9.4/tutorial-install.html) (`psql`)
* [Clojure](https://clojure.org/guides/getting_started)
* [Shadow CLJS](https://shadow-cljs.github.io/docs/UsersGuide.html#_standalone_via_code_npm_code)

Additionally, if you want to test out the 2-factor authentication, you'll need an API key from [Authy](https://www.twilio.com/docs/authy/api).

#### Setup

Get the RTC source code:

```sh
git clone git@github.com:breadsystems/rtc-care-schedule.git
cd rtc-care-schedule
```

Create the Postgres database:

```sh
createdb rtc
```

Configure the application by creating a `src/dev/config.edn` file:

```sh
cp src/dev/example.config.edn src/dev/config.edn
```

Edit the new `config.edn` file to set up 2FA. If you don't need to test backend authentication, you can disable authentication altogether by setting `:dev-disable-auth true`.

In most cases you can leave the `:database-url` value alone.

#### Running the dev environment

Once you've done all the above, you should be able to run:

```sh
clojure -A:dev -m rtc.app
```

If the app is able to start up correctly, you should be able to see it running at `localhost:8080`.

#### Tests

```sh
clojure -A:test
```

Add the `--watch` option to rerun tests when CLJ files change (Ctrl+C to exit).

#### The Shadow CLJS environment

This application uses a pretty standard shadow-cljs setup. To start the Shadow dev server:

```sh
shadow-cljs -A:dev server start
```

Go to `localhost:9630` to start watching the `app` and `test` builds. Once it's watching `test` you can go to `localhost:3002` to see CLJS test results, and even enable desktop notifications as test results come in.

### Lando installation (work in progress)

To build the dev environment, you will first need to install [Lando](https://docs.lando.dev/), the official dev environment for RTC. It's the best local dev tool in the galaxy! Note that Lando runs on top of Docker, but it will install a recent version of Docker for you if you don't already have it.

Next, get the RTC source code:

```sh
git clone git@github.com:breadsystems/rtc-care-schedule.git
cd rtc-care-schedule
```

Once Lando is installed, you can start the dev environment with a single command:

```sh
lando start 
```

This will initialize the database and start the dev environment. This may take a few minutes the first time, so be patient. When it's done, it will print the URL of the local RTC app to your terminal screen.

### TODO

* Link to Clojure guides
* Document basic app architecture

### License

Released under the [Anti-Capitalist Software License](https://anticapitalist.software/), v1.4