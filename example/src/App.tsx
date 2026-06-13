import { useState } from 'react';
import {
  Button,
  Image,
  SafeAreaView,
  ScrollView,
  StyleSheet,
  Text,
  View,
} from 'react-native';
import {
  launchCamera,
  launchImageLibrary,
  type Asset,
  type CameraType,
} from '@eugeniuszx/react-native-media-picker';

export default function App() {
  const [assets, setAssets] = useState<Asset[]>([]);
  const [status, setStatus] = useState('idle');
  const [facing, setFacing] = useState<CameraType>('back');

  const pick = (selectionLimit: number, includeBase64: boolean) => {
    setStatus('picking…');
    launchImageLibrary({
      selectionLimit,
      maxWidth: 640,
      maxHeight: 640,
      quality: 0.8,
      includeBase64,
    })
      .then((res) => {
        if (res.didCancel) {
          setStatus('cancelled');
          return;
        }
        if (res.errorCode) {
          setStatus(`error: ${res.errorCode} ${res.errorMessage ?? ''}`);
          return;
        }
        setStatus(`got ${res.assets?.length ?? 0} asset(s)`);
        setAssets(res.assets ?? []);
      })
      .catch((e: unknown) => {
        setStatus(`threw: ${String(e)}`);
      });
  };

  const capture = () => {
    setStatus('opening camera…');
    launchCamera({
      cameraType: facing,
      maxWidth: 1280,
      maxHeight: 1280,
      quality: 0.8,
    })
      .then((res) => {
        if (res.didCancel) {
          setStatus('cancelled');
          return;
        }
        if (res.errorCode) {
          setStatus(`error: ${res.errorCode} ${res.errorMessage ?? ''}`);
          return;
        }
        setStatus(`captured ${res.assets?.length ?? 0} asset(s)`);
        setAssets(res.assets ?? []);
      })
      .catch((e: unknown) => {
        setStatus(`threw: ${String(e)}`);
      });
  };

  return (
    <SafeAreaView style={styles.root}>
      <ScrollView contentContainerStyle={styles.content}>
        <Button title="Pick single" onPress={() => pick(1, false)} />
        <Button title="Pick multiple (5)" onPress={() => pick(5, false)} />
        <Button title="Pick single + base64" onPress={() => pick(1, true)} />
        <Button title={`Take photo (${facing})`} onPress={capture} />
        <Button
          title="Toggle front/back"
          onPress={() => setFacing((f) => (f === 'back' ? 'front' : 'back'))}
        />
        <Text style={styles.status}>{status}</Text>
        {assets.map((a) => (
          <View key={a.uri} style={styles.card}>
            <Image source={{ uri: a.uri }} style={styles.image} />
            <Text>{`${a.type} • ${a.width}x${a.height} • ${a.fileSize ?? '?'}B`}</Text>
            <Text numberOfLines={1}>{a.uri}</Text>
            <Text>{`base64: ${a.base64 ? `${a.base64.length} chars` : 'none'}`}</Text>
          </View>
        ))}
      </ScrollView>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  root: { flex: 1 },
  content: { padding: 16, gap: 12 },
  status: { fontWeight: '600', marginVertical: 8 },
  card: {
    gap: 4,
    borderWidth: 1,
    borderColor: '#ddd',
    padding: 8,
    borderRadius: 8,
  },
  image: { width: 160, height: 160, resizeMode: 'cover' },
});
