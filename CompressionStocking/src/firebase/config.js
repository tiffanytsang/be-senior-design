import * as firebase from 'firebase';
import '@firebase/auth';
import '@firebase/firestore';

const firebaseConfig = {
    apiKey: '***REMOVED***',
    authDomain: 'compressionstocking-90ddf.firebaseapp.com',
    databaseURL: 'https://compressionstocking-90ddf.firebaseio.com',
    projectId: 'compressionstocking-90ddf',
    storageBucket: 'compressionstocking-90ddf.appspot.com',
    messagingSenderId: '769297201074',
    appId: '1:769297201074:ios:245d0e279de035a878d6ab',
};

if (!firebase.apps.length) {
    firebase.initializeApp(firebaseConfig);
}

export { firebase };