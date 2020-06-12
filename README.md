# launch-cal
Space flight launch calendar parsing and calendar saving in one place.

## Setup
1. Set up Google credentials. If you will be running this as a CRON job make sure they are set up as a `service account`
2. Download P12 private key for your service account and rename it `launch-cal.p12`
3. Place the P12 file in the `src/main/resources` directory
4. Set up a Google calendar
    1. Copy the calendar ID, this will be used as a property later and looks similar to `<a-string-of-numbers-and-letters>@group.calendar.google.com`
    2. Share the calendar with the service account's email
    3. Edit notifications as you see fit
7. Set the properties in `properties.txt`
8. Run `gradle clean build`
9. Run the produced JAR file
