import React, { Component } from 'react';
import { StyleSheet, Text, ScrollView, View, Button, ActivityIndicator } from 'react-native';
import { RNCamera } from 'react-native-camera';

import MLKit from './components/MLKit';
import TesseractOcr from './components/TesseractOcr';

const FILE_PATH = 'engineplates/1.jpg';

export default class App extends Component {
  constructor(props) {
    super(props);

    this.state = {
      result: '',
      loading: false,
      showCamera: false,
      library: 'mlkit',
    };
  }
  onMlKit = (filePath) => {
    MLKit.detectInImage(filePath).then((res) => {
      this.setState({
        result: res,
        loading: false,
      });
    }).catch((err) => {
      this.setState({
        result: 'boo - ' + err,
        loading: false,
      });
    });
  }
  onTesseractOcr = (filePath) => {
    let tessOptions = {
      whitelist: null, //'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz1234567890.,-/_',
      blacklist: null,
    };

    setTimeout(() => {
      TesseractOcr.recognize(filePath, TesseractOcr.LANG_ENGLISH, tessOptions).then((res) => {
        console.log('f no');
        this.setState({
          result: res,
          loading: false,
        })
      }).catch((err) => {
        console.log('f no 2');
        this.setState({
          result: JSON.stringify(err),
          loading: false,
        });
      })
    });
  }
  runOCR(filePath) {
    this.setState({
      showCamera: false,
      result: '',
      loading: true,
    });

    if (this.state.library === 'tesseract') {
      this.onTesseractOcr(filePath);
    } else {
      this.onMlKit(filePath);
    }
  }
  onFile = () => {
    this.runOCR(FILE_PATH);
  }
  onCapture = () => {
    this.camera.takePictureAsync({
      quality: 1,
      fixOrientation: true,
    }).then((data) => {
      console.log(data);
      this.runOCR(data.uri);
    }).catch((err) => {
      console.log('Error: ' + err);
      this.setState({
        loading: false,
      });
    });
    this.setState({
      loading: true,
    });
  }
  render() {
    var data = [];
    if (typeof this.state.result === 'string' && this.state.result) {
      data.push(<Text key="res_string">{this.state.result}</Text>); 
    } else if (Array.isArray(this.state.result)) {
      this.state.result.forEach((block, i) => {
        data.push(<Text key={`block_${i}`}>----- Block: ({block.boundingBox.left},{block.boundingBox.top}) - ({block.boundingBox.right},{block.boundingBox.bottom}) -----</Text>);
        block.lines.forEach((line, i) => {
          data.push(<Text key={`line_${i}`} style={styles.line}>Line: {line}</Text>)
        });
      });
    }

    return (
      <ScrollView style={styles.scrollView}>
        {
          this.state.showCamera &&
          <RNCamera
            ref={(cam) => { this.camera = cam; }}
            style={styles.preview}
            type={RNCamera.Constants.Type.back}
            flashMode={RNCamera.Constants.FlashMode.off}
          >
             <Text style={styles.capture} onPress={this.onCapture}>
                [Run OCR]
             </Text>
          </RNCamera>
        }
        <View style={styles.view}>
          {
            this.state.loading &&
            <ActivityIndicator size="large" color="#0000ff" />
          }
          {
            !this.state.loading &&
            <ScrollView style={{height: (data.length > 0 ? 400 : 0), width: '100%'}}>
              {data}
            </ScrollView>
          }
          {
            !this.state.loading &&
            <View>
              <View style={styles.button}>
                <Button 
                  title={this.state.showCamera ? 'Hide Camera' : 'Show Camera'}
                  onPress={() => {
                    this.setState({showCamera: !this.state.showCamera, result: ''});
                  }}
                />
              </View>
              <View style={styles.button}>
                <Button title="Run OCR on File" onPress={this.onFile} />
              </View>
              <View style={styles.button}>
                <Button
                  title={this.state.library === 'tesseract' ? 'Switch to Google MLKit' : 'Switch to Tesseract OCR'}
                  onPress={() => {
                    this.setState({library: (this.state.library === 'tesseract' ? 'mlkit' : 'tesseract')})
                  }}
                />
              </View>
              <Text style={{textAlign: 'center'}}>{this.state.library === 'tesseract' ? 'Using Tesseract OCR' : 'Using Google MLKit'}</Text>
            </View>
          }
        </View>
      </ScrollView>
    );
  }
}

const styles = StyleSheet.create({
  scrollView: {
    flex: 1,
    backgroundColor: '#F5FCFF',
  },
  view: {
    paddingTop: 10,
    paddingLeft: 10,
    paddingRight: 10,
    paddingBottom: 20,
  },
  line: {
    fontWeight: 'bold',
  },
  button: {
    paddingBottom: 5,
    paddingTop: 5,
  },
  preview: {
    flex: 1,
    justifyContent: 'flex-end',
    alignItems: 'center',
    width: '100%',
    height: 300,
  },
  capture: {
    flex: 0,
    backgroundColor: '#fff',
    borderRadius: 5,
    color: '#000',
    padding: 10,
    margin: 40
  }
});
