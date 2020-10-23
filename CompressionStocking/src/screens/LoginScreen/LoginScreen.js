import React, { useState } from 'react'
import { Image, Text, TouchableOpacity, View } from 'react-native'
import { KeyboardAwareScrollView } from 'react-native-keyboard-aware-scroll-view';
import { firebase } from '../../firebase/config'
import * as GoogleSignIn from 'expo-google-app-auth';
import GoogleFit, { Scopes } from 'react-native-google-fit'
import { LinearGradient } from 'expo-linear-gradient'
import styles from './styles';

export default function LoginScreen({ navigation }) {
  const [user, setUser] = useState(null);

  const onFooterLinkPress = () => {
    navigation.navigate('Registration')
  }

  const loginInWithGoogle = async () => {
    try {
      const loginResult = await GoogleSignIn.logInAsync({
        androidClientId: '769297201074-iioa7crqlu38alosdcob7ofr9ih6iq56.apps.googleusercontent.com',
        iosClientId: '769297201074-a04dnnfsubn3fn14i7k66q20iv3c9ln2.apps.googleusercontent.com',
        scopes: ['profile', 'email', Scopes.FITNESS_ACTIVITY_READ_WRITE, Scopes.FITNESS_BODY_READ_WRITE,],
      });

      if (loginResult.type === 'success') {
        let userInfoResponse = await fetch('https://www.googleapis.com/userinfo/v2/me', {
          headers: { Authorization: `Bearer ${loginResult.accessToken}` },
        });
        setUser(loginResult);

        const usersRef = firebase.firestore().collection('users');
        firebase.auth().onAuthStateChanged(loginResult => {
          usersRef
            .doc(loginResult.uid)
            .get()
            .then((document) => {
              const userData = document.data()
              // console.log("firbase user data", userData) // this is wrong this returns tiffany@email.com
            })
            .catch((error) => {
              console.log("User not found in firebase.")
            });
        });

        navigation.navigate("Home", { loginResult })
      } else {
        console.log("error logging in")
        // return { cancelled: true };
      }
    } catch (e) {
      console.log("Error from line 49ish", e)
      return { error: true };
    }
  }

  return (
    <View style={styles.container}>
      <LinearGradient
        colors={['#e5f8e5', '#b6edb6', '#67d967']}
        style={{ flex: 1 }}
      >
        <KeyboardAwareScrollView
          style={{ flex: 1, width: '100%' }}
          keyboardShouldPersistTaps="always">
          <Image
            style={styles.logo}
            source={require('../../../assets/icon.jpg')}
          />
          <Text style={styles.title}>Dynamic Compression Sock</Text>
          <TouchableOpacity
            style={styles.button}
            onPress={() => loginInWithGoogle()}>
            <Text style={styles.buttonTitle}>Log in</Text>
          </TouchableOpacity>
          <View style={styles.footerView}>
            <Text style={styles.footerText}>Don't have an account? <Text onPress={onFooterLinkPress} style={styles.footerLink}>Sign up</Text></Text>
          </View>
        </KeyboardAwareScrollView>
      </LinearGradient>
    </View>

  )
}