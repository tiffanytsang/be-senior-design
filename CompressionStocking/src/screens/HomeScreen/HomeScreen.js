import React, { useEffect, useState } from 'react'
import { FlatList, Keyboard, Text, TextInput, TouchableOpacity, View } from 'react-native'
import { firebase } from '../../firebase/config'
import * as GoogleSignIn from 'expo-google-app-auth';
import { LinearGradient } from 'expo-linear-gradient'
import styles from './styles';

export default function HomeScreen(props) {

    const [entityText, setEntityText] = useState('')
    const [entities, setEntities] = useState([])
    const [mostRecentHeartRate, setMostRecentHeartRate] = useState(-1)

    // console.log("These are props from line 13", props)

    const entityRef = firebase.firestore().collection('entities')
    const userID = props.route.params.loginResult.user.id

    useEffect(() => {
        googleFit();
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

    const googleFit = async () => {
        console.log("enters google fit")
        const startTime = (new Date()).getTime() * 1000000 - (10 ** 13)
        const endTime = (new Date()).getTime() * 1000000
        console.log("This should be a current time", endTime)
        const url = 'https://www.googleapis.com/fitness/v1/users/me/dataSources/derived:com.google.heart_rate.bpm:com.google.android.gms:merge_heart_rate_bpm/datasets/' + startTime + '-' + endTime;
        // const url = 'https://www.googleapis.com/fitness/v1/users/me/dataSources/raw:com.google.heart_rate.bpm:com.google.android.apps.fitness:Compal:Falster 2:2e99fb6c:manual/datasets/' + startTime + '-' + endTime;
        const bearer = 'Bearer ' + props.route.params.loginResult.accessToken;
        const fitness = await fetch(url, {
            method: 'GET',
            withCredentials: true,
            credentials: 'include',
            headers: {
                'Authorization': bearer,
                'X-FP-API-KEY': 'iphone', //it can be iPhone or your any other attribute
                'Content-Type': 'application/json; charset=utf-8'
            }
        })
        const fitnessResponse = await fitness.json();
        console.log("most recent heart rate", fitnessResponse["point"].slice(-1)[0]["value"][0]["fpVal"])
        setMostRecentHeartRate(fitnessResponse["point"].slice(-1)[0]["value"][0]["fpVal"])
    }

    const signOut = async () => {
        try {
            const accessToken = props.route.params.loginResult.accessToken;
            const config = {
                androidClientId: '769297201074-iioa7crqlu38alosdcob7ofr9ih6iq56.apps.googleusercontent.com',
                iosClientId: '769297201074-a04dnnfsubn3fn14i7k66q20iv3c9ln2.apps.googleusercontent.com',
            };
            await GoogleSignIn.logOutAsync({ accessToken, ...config });;
            props.navigation.navigate("Login")
        } catch (error) {
            console.error(error);
        }
    };

    return (
        <LinearGradient
            colors={['#e5f8e5', '#b6edb6', '#67d967']}
            style={{ flex: 1 }}
        >
            <View style={styles.container}>

                {
            /* <View style={styles.formContainer}>
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

                <Text style={styles.entityText}>Hi {props.route.params.loginResult.user.givenName}!</Text>
                {/* <TouchableOpacity
                style={styles.button}
                onPress={() => googleFit()}>
                <Text style={styles.buttonTitle}>Try Google fit</Text>
            </TouchableOpacity> */}
                <View style={styles.space} />
                <View style={styles.space} />
                <View style={styles.space} />
                <Text style={styles.entityText}>Most recent heart rate: {mostRecentHeartRate}</Text>
                <View style={styles.space} />
                <View style={styles.space} />
                <View style={styles.space} />
                <TouchableOpacity
                    style={styles.button}>
                    <Text style={styles.buttonTitle}>Compress</Text>
                </TouchableOpacity>
                <View style={styles.space} />
                <TouchableOpacity
                    style={styles.button}>
                    <Text style={styles.buttonTitle}>Decompress</Text>
                </TouchableOpacity>
                <View style={styles.space} />
                <View style={styles.space} />
                <View style={styles.space} />
                <View style={styles.space} />
                <View style={styles.space} />
                <View style={styles.space} />
                <View style={styles.space} />
                <View style={styles.space} />
                <TouchableOpacity
                    style={styles.button}
                    onPress={() => signOut()}>
                    <Text style={styles.buttonTitle}>Logout</Text>
                </TouchableOpacity>

            </View >
        </LinearGradient>
    )
}