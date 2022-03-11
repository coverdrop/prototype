# CoverDrop

This repository contains the source code for the ["CoverDrop: Blowing the Whistle Through A News App" paper published at PETS](https://petsymposium.org/2022/files/papers/issue2/popets-2022-0035.pdf).

Keep in mind that this is an academic prototype.
This is not fit for any real-world usage.
Also, components (such as UI, registration, ...) that are not central to the paper are mocked or using hard-coded values.



## Setup

The individual subfolders for the SGX, WebAPI, MobileApp, and Simulators have dedicated `README.md` files with setup instructions.

Since this is a clone of our experimental prototype you will find hard-coded identifier and URLs that need updating for your infrastructure setup.

## Demonstrated functionality

This prototype currently demonstrates the following functionality:

 - Overall: For one hard-coded user and one hard-coded reporter a complete message pass through the SGX without encryption
 - SGX: Requesting data from the web api server and processing it inside the enclave
 - SGX: Returning data from the enclave and publishing it through the web api server
 - WebAPI: Providing a complete API for handling the messages from/to users/reporters/SGX
 - WebAPI: Providing an API for demo news articles for the prototype
 - WebAPI: Providing rudimentary persistence (sqlite) and authentication (hard-coded auth tokens)
 - MobileApp: An Android library that allows secure and deniable storage on disk using strong key derivation (argon2) and key-splitting with the secure element
 - MobileApp: A prototype app that displays news articles and reporters
 - MobileApp: A prototype app that allows locking/unlocking an encrypted state with secret split between passphrase and secure element
 - MobileApp: A prototype app that demonstrates a regular background service for sending dummy traffic
 - MobileApp: A prototype app that demonstrates encrypting and decrypting messages as described
 - Simulators: Generating all keys necessary (SGX, reporters, ...)
 - Simulators: Simulating a user (sending real and dummy messages, encryption/decryption)
 - Simulators: Simulating a SGX (acting as a threshold mix, encryption/decryption)
 - Simulators: Simulating a reporter (sending real replies and dummy messages, encryption/decryption)

This prototype currently does NOT demonstrate the following functionality:

 - SGX: Any encryption or decryption of messages (these operations were shown with a different project that is not included here)
 - WebAPI: Any registration, advanced authentication, or scalability
 - MobileApp: Any registration or account authentication

## Citation

You can use the following BibTex for citations:

```
@article{ahmed2022coverdrop,
  title={CoverDrop: Blowing the Whistle Through A News App},
  author={Ahmed-Rengers, Mansoor and Vasile, Diana A and Hugenroth, Daniel and Beresford, Alastair R and Anderson, Ross},
  journal={Proceedings on Privacy Enhancing Technologies},
  volume={2022},
  number={2},
  pages={47--67},
  year={2022}
}
```
