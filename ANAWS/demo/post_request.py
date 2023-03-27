import sys
import requests
import argparse

endpoint = "http://192.168.56.103:8080/dc/network/reserve/flow/json"

def do_post_request(src_ip, src_mac, dest_ip, dest_mac, data_load):
    my_obj = {"srcIp": src_ip, "srcMac": src_mac, "destIp": dest_ip, "destMac": dest_mac, "dataload": data_load}
    res = requests.post(endpoint, json=my_obj)
    return res

def main():
    parser = argparse.ArgumentParser(description="post request for a flow reservation")

    parser.add_argument("src_ip", help="ipv4 address of the source host")
    parser.add_argument("src_mac", help="mac address of the source host")
    parser.add_argument("dest_ip", help="ipv4 address of the destination host")
    parser.add_argument("dest_mac", help="mac address of the destination host")
    parser.add_argument("data_load", help="data load (GiB) to send", type=float)

    args = parser.parse_args()
    print(args)

    src_ip = args.src_ip
    src_mac = args.src_mac
    dest_ip = args.dest_ip
    dest_mac = args.dest_mac
    data_load = args.data_load

    res = do_post_request(src_ip, src_mac, dest_ip, dest_mac, data_load)
    if not res.ok:
        print(res.text)
        sys.exit(-1)
    else:
        print(res.text)
        sys.exit(0)

if __name__ == "__main__":
    main()
