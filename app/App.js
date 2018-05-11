import React, { Component } from 'react';
import { StyleSheet, Text, ScrollView, View, Button, ActivityIndicator } from 'react-native';

import MLKit from './components/MLKit';
import TesseractOcr from './components/TesseractOcr';

export default class App extends Component {
  constructor(props) {
    super(props);

    this.state = {
      result: '',
      loading: false,
    };
  }
  onMlKit = () => {
    this.setState({
      loading: true,
    });

    MLKit.detectInImage('engineplates/1.jpg').then((res) => {
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
  onTesseractOcr = () => {
    let tessOptions = {
      whitelist: null, //'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz1234567890.-/_',
      blacklist: null,
    };

    this.setState({
      loading: true,
    });

    setTimeout(() => {
      TesseractOcr.recognize('engineplates/1.jpg', TesseractOcr.LANG_ENGLISH, tessOptions).then((res) => {
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
  render() {
    console.log('wtf...' + (this.state.loading ? 'loading' : 'not loading'));
    var data = [];
    if (typeof this.state.result === 'string') {
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
        <View style={styles.view}>
          {
            this.state.loading &&
            <ActivityIndicator size="large" color="#0000ff" />
          }
          {
            !this.state.loading &&
            data
          }
          {
            !this.state.loading &&
            <View style={styles.button}>
              <Button title="Tesseract OCR" onPress={this.onTesseractOcr} />
            </View>
          }
          {
            !this.state.loading &&
            <View style={styles.button}>
              <Button title="Google MLKit" onPress={this.onMlKit} />
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
    paddingBottom: 20,
  },
  line: {
    fontWeight: 'bold',
  },
  button: {
    paddingBottom: 10,
    paddingTop: 10,
  }
});
