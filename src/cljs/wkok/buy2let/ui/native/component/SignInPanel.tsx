import * as React from 'react';
import { View } from 'react-native';
import { MD3LightTheme, Button, Dialog, Portal, Provider as PaperProvider, TextInput } from 'react-native-paper';

const theme = {
  ...MD3LightTheme, // or MD3DarkTheme
  roundness: 1,
  colors: {
    ...MD3LightTheme.colors,
    primary: '#2196f3'
  },
};

export default function SignInPanel(props: any) {

  const { onPress } = props;

  return (
    <PaperProvider theme={theme}>
      <View>
        <Portal>
            <Dialog dismissable={false} visible={true} style={{width: 250, alignSelf: "center"}}>
            <Dialog.Title>Sign in</Dialog.Title>
            <Dialog.Content>
               <TextInput label="Email" value="demo@email.com" disabled={true}/>
               <TextInput label="Password" value="***********" disabled={true}/>
            </Dialog.Content>
            <Dialog.Actions style={{alignSelf: "center"}}>
              <Button onPress={onPress} mode="contained">SIGN IN</Button>
            </Dialog.Actions>
          </Dialog>
        </Portal>
      </View>
    </PaperProvider>
  );
};
