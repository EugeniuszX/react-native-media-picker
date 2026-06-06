import { useState } from 'react';
import { Button, Text, View, StyleSheet } from 'react-native';
import { launchImageLibrary } from '@eugeniuszx/react-native-media-picker';

export default function App() {
  const [status, setStatus] = useState('—');

  const pick = async () => {
    const result = await launchImageLibrary({ mediaType: 'photo' });
    if (result.didCancel) {
      setStatus('cancelled');
    } else if (result.errorCode) {
      setStatus(`error: ${result.errorCode}`);
    } else {
      setStatus(`picked ${result.assets?.length ?? 0} asset(s)`);
    }
  };

  return (
    <View style={styles.container}>
      <Button title="Pick photo" onPress={pick} />
      <Text style={styles.status}>{status}</Text>
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    alignItems: 'center',
    justifyContent: 'center',
    gap: 16,
  },
  status: {
    fontSize: 16,
    color: '#333',
  },
});
