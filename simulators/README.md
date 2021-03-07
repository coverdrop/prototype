# Simulators

This folder contains helpful simulators for testing and development.
In particular, we provide a `sim_user.py`, `sim_reporter.py`, and a `sim_sgx.py` file.

Observed from the outside they perform the same operations as their real implementations, but they allow for easier code and deployment.

The `utils.py` file can be used to create new key material.


## How to run

Initial setup:

```
$ python3 -mvenv env
$ source env/bin/activate
(env) $ python3 -mpip install -r requirements.txt
```

Running the simulators (in different terminals):

```
(env) $ python3 sim_user.py
(env) $ python3 sim_reporter.py
(env) $ python3 sim_sgx.py
```