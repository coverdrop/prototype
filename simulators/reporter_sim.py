import time

def tick():
    pass

def run():
    try:
        while True:
            tick()
            time.sleep(1)
    except KeyboardInterrupt:
        print("Received CTRL+C")

if __name__=="__main__":
    run()