# launch-cal
Space flight launch calendar parsing and calendar saving in one place.

##Setup
1. Create `private.json` in the `src/resources` folder to hold the private key and other 
    various fields for Google Calendar connection
2. Run `gradle clean build`
3. Set up Google credentials. If you will be running this as a CRON job make sure they are set up as a `service account`
4. Download the client secret file as JSON and rename it to `client_secret.json`
5. Place the secret file in the same directory as the JAR file
6. Set up a Google calendar
    1. Copy the calendar ID, this will be used as a property later and looks similar to `<a-string-of-numbers-and-letters>@group.calendar.google.com`
    2. Share the calendar with the email address listed in your client secret file under the `client_email` field (ensuring that the permissions are set to `Make changes to everything`)
    3. Edit notifications as you see fit
7. Set the properties in `properties.txt`
8. Run the produced JAR file