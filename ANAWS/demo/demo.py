import sys
import requests

ip_controller = "http://8.8.8.8:8080/dc/network/flow/json"

def do_post_request(ip, dest, data_load):
    my_obj = {"dest": dest, "dataload": data_load}
    res = requests.post(ip, json=my_obj)
    return res

def main(argv):
    dest = argv[1]
    data_load = argv[2]
    res = do_post_request(ip_controller, dest, data_load)
    if not res.ok:
        print(res.text)
        sys.exit(-1)
    else:
        print(res.text)
        sys.exit(0)

if __name__ == "__main__":
    if len(sys.argv) < 3:
        print("You have to pass 2 parameter")
        sys.exit()
    main(sys.argv[1:])
