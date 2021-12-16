# RTC Care Schedule

Calendar and scheduling system for the Radical Telehealth Collective

### Features

* Frontend for people seeking medical care to book an appointment and enter their info
* Backend calendar for medical providers to schedule their availability
* Scheduling application for volunteers to communicate with careseekers and coordinate access needs such as interpreters, captioners, etc.

## Running in production

This application ships an "uberjar," which is the recommended method of running in production. The build script outputs the uberjar to `target/rtc.jar`, which can then be run just like any other .jar file:

```sh
DATABASE_URL=your-db-url AUTHY_API_KEY=your-api-key \
  java -cp target/rtc.jar clojure.main -m rtc.app # run the jar
```

## Building

The build script contains everything you need to build the production uberjar. Just run it:

```sh
bin/build
```

This application is hosted on Heroku. To deploy, build the uberjar as describe above and run:

```sh
heroku deploy:jar target/rtc.jar --app radical-telehealth-collective
```

You will need to be logged in to the [Heroku CLI](https://devcenter.heroku.com/articles/heroku-cli) to do this.

## Development

### Local install prerequisites

* [PostgreSQL](https://www.postgresql.org/docs/9.4/tutorial-install.html) (`psql`)
* [Clojure](https://clojure.org/guides/getting_started)
* [Shadow CLJS](https://shadow-cljs.github.io/docs/UsersGuide.html#_standalone_via_code_npm_code)

Additionally, if you want to test out the 2-factor authentication, you'll need an API key from [Authy](https://www.twilio.com/docs/authy/api).

### Setup

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

Edit the new `config.edn` file to set up 2FA with your [Authy API key](https://www.twilio.com/docs/authy/api#authy-api-keys). If you want to skip this for development purposes, see the section below about *security settings in dev*.

In most cases you can leave the `:database-url` value alone.

### Running the dev environment

Start a REPL from your editor and load the file `src/rtc/app.clj`. This is the main entrypoint for the application.

Find and evaluate the `(mount/start)` form at the beginning of the `comment` form towards the end of the file. This should perform any necessary database migrations and warn you in the REPL window in case you disabled auth/anti-forgery protection in your config. If no admin test user exists, it will also create one for you and print the credentials to the REPL window.

If the app is able to start up correctly, you should be able to see it running at `localhost:8080`.

From there, you can...

* Click **Get Care** to test out the intake process.
* Visit `/comrades` to test out the admin

### Security settings in dev

In a development environment, it's possible to disable backend authentication in case you are not actively testing that feature. You can disable authentication altogether by setting `:dev-disable-auth true`. You can also disable anti-XSRF protection with `:dev-disable-anti-forgery true`.

Obviously you should never override the default values for these in production.

### Tests

```sh
clojure -A:test
```

Add the `--watch` option to rerun tests when CLJ files change (Ctrl+C to exit).

### The Shadow CLJS environment

This application uses a pretty standard shadow-cljs setup. To start the Shadow dev server:

```sh
shadow-cljs -A:dev server start
```

Go to `localhost:9630` to start watching the `app` and `test` builds. Once it's watching `test` you can go to `localhost:3002` to see CLJS test results, and even enable desktop notifications as test results come in.

## TODO

* Link to Clojure guides
* Document basic app architecture

## License

Released under the [Anti-Capitalist Software License](https://anticapitalist.software/), v1.4
