
const express = require('express');
const superagent = require('superagent');

const CLIENT_ID = process.env['CLIENT_ID'];
if (!CLIENT_ID) {
  console.error('CLIENT_ID environment variable needs to be set.');
  process.exit(1);
}

const CLIENT_SECRET = process.env['CLIENT_SECRET'];
if (!CLIENT_SECRET) {
  console.error('CLIENT_SECRET environment variable needs to be set.')
}

function base64Encode(input) {
  return Buffer.from(input).toString('base64');
}

  const auth = `${CLIENT_ID}:${CLIENT_SECRET}`;
  superagent
    .post('https://api-m.sandbox.paypal.com/v1/oauth2/token?grant_type=client_credentials')
    .set('Accept', 'application/json')
    .set('Accept-Language', 'en_US')
    .set('Authorization', `Basic ${ base64Encode(auth) }`)
    .end((err, res) => {
      console.log(res.status);
      console.log(res.body);
    });

const app = express();
app.post('/client_token', (req, res) => {
  const auth = `${CLIENT_ID}:${CLIENT_SECRET}`;

  superagent
    .post('https://api-m.sandbox.paypal.com/v1/oauth2/token?grant_type=client_credentials')
    .set('Accept', 'application/json')
    .set('Accept-Language', 'en_US')
    .set('Authorization', `Basic ${ base64Encode(auth) }`)
    .end((tokenErr, tokenRes) => {
      res.status(tokenRes.status).send(tokenRes.body);
    });
});

const port = 8080;
app.listen(port, () => {
  console.log(`Example app listening at http://localhost:${port}`);
});
