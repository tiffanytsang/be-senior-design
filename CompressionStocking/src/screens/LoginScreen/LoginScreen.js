import React, { useState , useEffect} from 'react'
import { firebase } from '../../firebase/config'
import { Image, Text, TextInput, TouchableOpacity, View } from 'react-native'
import { KeyboardAwareScrollView } from 'react-native-keyboard-aware-scroll-view';
import styles from './styles';
import * as GoogleSignIn from 'expo-google-app-auth';

export default function LoginScreen({ navigation }) {
    const [user, setUser] = useState(null);


    const onFooterLinkPress = () => {
        navigation.navigate('Registration')
    }

    useEffect(() => {
        signInWithGoogleAsync();
     }, []);

    const signInWithGoogleAsync = async () => {
      try {
        const result = await GoogleSignIn.logInAsync({
          androidClientId: '769297201074-iioa7crqlu38alosdcob7ofr9ih6iq56.apps.googleusercontent.com',
          iosClientId: '769297201074-a04dnnfsubn3fn14i7k66q20iv3c9ln2.apps.googleusercontent.com',
          scopes: ['profile', 'email', ],
        });

        if (result.type === 'success') {
            let userInfoResponse = await fetch('https://www.googleapis.com/userinfo/v2/me', {
                headers: { Authorization: `Bearer ${result.accessToken}` },
            });
            console.log(result)
            setUser(result);
          // return result.accessToken;
        } else {
            console.log("error logging in")
            // return { cancelled: true };
        }
      } catch (e) {
        console.log(e)
        return { error: true };
      }
    }


    const signOutAsync = async () => {
        await GoogleSignIn.signOutAsync();
        setUser(null);
    };


    const onPress = () => {
        if (user) {
          signOutAsync();
        } else {
            signInWithGoogleAsync();
        }
    };

    return(
        <Text onPress={onPress}>Toggle Auth</Text>
    );

    // render() {
    //     return <Text onPress={this.onPress}>Toggle Auth</Text>;
    // }
}



    // const onLoginPress = () => {
    //     firebase
    //         .auth()
    //         .signInWithEmailAndPassword(email, password)
    //         .then((response) => {
    //             const uid = response.user.uid
    //             const usersRef = firebase.firestore().collection('users')
    //             usersRef
    //                 .doc(uid)
    //                 .get()
    //                 .then(firestoreDocument => {
    //                     if (!firestoreDocument.exists) {
    //                         alert("User does not exist anymore.")
    //                         return;
    //                     }
    //                     const user = firestoreDocument.data()
    //                     navigation.navigate('Home', { user })
    //                 })
    //                 .catch(error => {
    //                     alert(error)
    //                 });
    //         })
    //         .catch(error => {
    //             alert(error)
    //         })
    // }

    // return (
    //     <View style={styles.container}>
    //         <KeyboardAwareScrollView
    //             style={{ flex: 1, width: '100%' }}
    //             keyboardShouldPersistTaps="always">
    //             <Image
    //                 style={styles.logo}
    //                 source={require('../../../assets/icon.png')}
    //             />
    //             <TextInput
    //                 style={styles.input}
    //                 placeholder='E-mail'
    //                 placeholderTextColor="#aaaaaa"
    //                 onChangeText={(text) => setEmail(text)}
    //                 value={email}
    //                 underlineColorAndroid="transparent"
    //                 autoCapitalize="none"
    //             />
    //             <TextInput
    //                 style={styles.input}
    //                 placeholderTextColor="#aaaaaa"
    //                 secureTextEntry
    //                 placeholder='Password'
    //                 onChangeText={(text) => setPassword(text)}
    //                 value={password}
    //                 underlineColorAndroid="transparent"
    //                 autoCapitalize="none"
    //             />
    //             <TouchableOpacity
    //                 style={styles.button}
    //                 onPress={() => onLoginPress()}>
    //                 <Text style={styles.buttonTitle}>Log in</Text>
    //             </TouchableOpacity>
    //             <View style={styles.footerView}>
    //                 <Text style={styles.footerText}>Don't have an account? <Text onPress={onFooterLinkPress} style={styles.footerLink}>Sign up</Text></Text>
    //             </View>
    //         </KeyboardAwareScrollView>
    //     </View>
    // )