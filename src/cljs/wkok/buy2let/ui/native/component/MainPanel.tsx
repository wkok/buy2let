import React from 'react';
import { View } from 'react-native';
import { MD3LightTheme, Provider as PaperProvider, Text, Button} from 'react-native-paper';
import { NavigationContainer } from '@react-navigation/native';

const theme = {
  ...MD3LightTheme, // or MD3DarkTheme
  roundness: 1,
  colors: {
    ...MD3LightTheme.colors,
    primary: '#2196f3'
  },
};


export default function MainPanel(props: any) {

  const { onPress } = props;

  return (
    <PaperProvider theme={theme}>
      <NavigationContainer>
        <View style={{alignSelf: "center", padding: 50}}>
          <Text variant="displayLarge">Dashboard</Text>
          <Text variant="displaySmall">(Under Construction)</Text>
        </View>
        <Button onPress={onPress} mode="contained" style={{alignSelf: "center", width: 150}}>SIGN OUT</Button>

      </NavigationContainer>
    </PaperProvider>
  );
}
