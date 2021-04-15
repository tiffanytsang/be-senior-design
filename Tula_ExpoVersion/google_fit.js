import * as Google from 'expo-google-app-auth';
import GoogleFit, { Scopes } from 'react-native-google-fit'


//logging in works
async function signInWithGoogleAsync() {
  try {
    const result = await Google.logInAsync({
      androidClientId: '134054703772-eeo4cc7v04o471dtsijhpg8shtnck1fh.apps.googleusercontent.com';
      iosClientId: '134054703772-fe6jleadmqt38anojmug3ers0rt0ethc.apps.googleusercontent.com',
      scopes: ['profile', 'email', Scopes.FITNESS_ACTIVITY_READ_WRITE,, Scopes.FITNESS_BODY_READ_WRITE, 'https://www.googleapis.com/auth/fitness.body.read',
        'https://www.googleapis.com/auth/fitness.body.write'],
    });

    if (result.type === 'success') {
      console.log("logged in!")
    } else {

      console.log("failed to log in")
    }

    let userInfoResponse = await fetch('https://www.googleapis.com/userinfo/v2/me', {
      headers: { Authorization: `Bearer ${result.accessToken}` },
    });

    const resp = await userInfoResponse.json(); // this works and returns user info
    console.log(resp)
}



//None of this works rip
GoogleFit.checkIsAuthorized().then(() => {
  console.log(GoogleFit.isAuthorized) // Then you can simply refer to `GoogleFit.isAuthorized` boolean.
})

const options = {
  scopes: [
    Scopes.FITNESS_ACTIVITY_READ_WRITE,
    Scopes.FITNESS_BODY_READ_WRITE,
  ],
}

GoogleFit.authorize(options)
  .then(authResult => {
    if (authResult.success) {
      dispatch("AUTH_SUCCESS");
    } else {
      dispatch("AUTH_DENIED", authResult.message);
    }
  })
  .catch(() => {
    dispatch("AUTH_ERROR");
  })

GoogleFit.startRecording((callback) => {
  // Process data from Google Fit Recording API (no google fit app needed)
});

const options = {
  startDate: "2020-10-11T00:00:17.971Z", // required
  endDate: new Date().toISOString(), // required
  bucketUnit: "DAY", // optional - default "DAY". Valid values: "NANOSECOND" | "MICROSECOND" | "MILLISECOND" | "SECOND" | "MINUTE" | "HOUR" | "DAY"
  bucketInterval: 1, // optional - default 1.
}

const callback = ((error, response) => {
  console.log(error, response)
});

GoogleFit.getHeartRateSamples(options, callback)
GoogleFit.getBloodPressureSamples(options, callback)

}