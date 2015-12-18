'use strict';
const should = require('should');
const assert = require('assert');
const helpers = require('./helpers/index');

describe('App: on message event "location"', function () {

  before(() => {
    helpers.setupCache();
    const app = require('../bin/www');
  });

  it('should publish user location', done => {
    /*
     Beacon locations (1,2,3) are in config/test.json
     ClientA location should be in the center: 2,2

     +---+---+---+
     |   |   |   |
     | 1 |   | 2 |
     |   |   |   |
     +-----------+
     |   |   |   |
     |   | A |   |
     |   |   |   |
     +-----------+
     |   |   |   |
     | 3 |   |   |
     |   |   |   |
     +---+---+---+
     */

    const CLIENT_A_LOCATION = {email: 'ClientA', type: 'location', x: 2, y: 2};
    const clientA = helpers.createSocketConnection();
    const clientB = helpers.createSocketConnection();

    clientA.on('connect', () => {
      clientA.emit('message', {type:'location', email: 'ClientA', id: 1, distance: 1, floor: 1});
      clientA.emit('message', {type:'location', email: 'ClientA', id: 2, distance: 1, floor: 1});
      clientA.emit('message', {type:'location', email: 'ClientA', id: 3, distance: 1, floor: 1});

      clientA.on('location', message => {
        should(message).deepEqual(CLIENT_A_LOCATION);
        done();
      });

      clientA.disconnect();
    });

    clientB.on('message', message => {
      should(message).deepEqual(CLIENT_A_LOCATION);
      clientB.disconnect();
      done();
    });

  });

});
