import React, { useEffect, useState } from 'react'
import { FlatList, Keyboard, Text, TextInput, TouchableOpacity, View } from 'react-native'
import styles from './styles';
import { firebase } from '../../firebase/config'
import * as GoogleSignIn from 'expo-google-app-auth';

export default function HomeScreen(props) {

    const [entityText, setEntityText] = useState('')
    const [entities, setEntities] = useState([])

    // console.log("These are props from line 12", props)

    const entityRef = firebase.firestore().collection('entities')
    const userID = props.route.params.loginResult.user.id

    useEffect(() => {
        entityRef
            .where("authorID", "==", userID)
            .orderBy('createdAt', 'desc')
            .onSnapshot(
                querySnapshot => {
                    const newEntities = []
                    querySnapshot.forEach(doc => {
                        const entity = doc.data()
                        entity.id = doc.id
                        newEntities.push(entity)
                    });
                    setEntities(newEntities)
                },
                error => {
                    console.log(error)
                }
            )
    }, [])

    const onAddButtonPress = () => {
        if (entityText && entityText.length > 0) {
            const timestamp = firebase.firestore.FieldValue.serverTimestamp();
            const data = {
                text: entityText,
                authorID: userID,
                createdAt: timestamp,
            };
            entityRef
                .add(data)
                .then(_doc => {
                    setEntityText('')
                    Keyboard.dismiss()
                })
                .catch((error) => {
                    alert(error)
                });
        }
    }

    const renderEntity = ({ item, index }) => {
        return (
            <View style={styles.entityContainer}>
                <Text style={styles.entityText}>
                    {index}. {item.text}
                </Text>
            </View>
        )
    }

    const signOut = async () => {
        try {
            // await GoogleSignIn.revokeAccess();
            const accessToken = props.route.params.loginResult.accessToken;
            const config = {
                androidClientId: '769297201074-iioa7crqlu38alosdcob7ofr9ih6iq56.apps.googleusercontent.com',
                iosClientId: '769297201074-a04dnnfsubn3fn14i7k66q20iv3c9ln2.apps.googleusercontent.com',
            };
            await GoogleSignIn.logOutAsync({ accessToken, ...config });;
            // auth()
            //     .signOut()
            //     .then(() => alert('Your are signed out!'));
            // setloggedIn(false);
            // setuserInfo([]);
            props.navigation.navigate("Login")
        } catch (error) {
            console.error(error);
        }
    };

    return (
        <View style={styles.container}>
            {/* <View style={styles.formContainer}>
                <TextInput
                    style={styles.input}
                    placeholder='Add new entity'
                    placeholderTextColor="#aaaaaa"
                    onChangeText={(text) => setEntityText(text)}
                    value={entityText}
                    underlineColorAndroid="transparent"
                    autoCapitalize="none"
                />
                <TouchableOpacity style={styles.button} onPress={onAddButtonPress}>
                    <Text style={styles.buttonText}>Add</Text>
                </TouchableOpacity>
            </View>
            { entities && (
                <View style={styles.listContainer}>
                    <FlatList
                        data={entities}
                        renderItem={renderEntity}
                        keyExtractor={(item) => item.id}
                        removeClippedSubviews={true}
                    />
                </View>
            )} */}
            <Text style={styles.entityText}>Welcome home {props.route.params.loginResult.user.givenName}!</Text>
            <TouchableOpacity
                style={styles.button}
                onPress={() => signOut()}>
                <Text style={styles.buttonTitle}>Logout</Text>
            </TouchableOpacity>
        </View>
    )
}