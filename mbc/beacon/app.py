from flask import Flask, request, Response
import json, sqlite3, os, time, random
from threading import Thread

# IMPORTANT
# The program is insecure against multiple registrations. It is created only to give beacon to honest nodes. 
# For MVP attacks against beacon are taken as out of scope
# Production-ready implementation must have:
#   1. Protection against DOS by registrations
#   2. Possibility to use Access Control Lists - to register it is necessary to provide signed certificate, nodes with revoked certificates cannot build blocks
#   3. Possibility to listen to the ledger: if chosen node has not created a block in consequetive N rounds, exclude it from list

database = "database.db"
period = 10
app = Flask(__name__)

try:
    os.remove(database)
except:
    pass
os.mknod(database)

class ConsensusRoutine(Thread):
    def __init__(self):
        super().__init__()

    def run(self):
        while True:
            conn = sqlite3.connect(database)
            cursor = conn.cursor()
            cursor.execute("SELECT COUNT(*) from nodes")
            number_of_nodes = cursor.fetchall()[0][0]
            if number_of_nodes > 0:
                selected = random.randint(1, number_of_nodes)
                cursor.execute("INSERT INTO consensus_order (node_id) VALUES (?)", (selected,))
                conn.commit()
                # print(f"N: {number_of_nodes}, S: {selected}")
            time.sleep(period)


conn = sqlite3.connect(database)
cursor = conn.cursor()
cursor.execute("CREATE TABLE nodes (id INTEGER primary key autoincrement, address text)")
cursor.execute("CREATE TABLE consensus_order (id INTEGER primary key autoincrement, node_id INTEGER NOT NULL REFERENCES nodes(id))")
conn.commit()

@app.route("/register", methods=["POST"])
def register():
    data = json.loads(request.data)
    response = {}
    if not "address" in data:
        response["status"] = "error"
        response["message"] = "You must specify label and address in mvp version"
        code = 400
    else:
        conn = sqlite3.connect(database)
        cursor = conn.cursor()
        res = cursor.execute("SELECT * FROM nodes").fetchall()
        marker = True
        for r in res:
            if r[1] == data["address"]:
                marker = False
                break
        if marker:
            cursor.execute("INSERT INTO nodes (address) VALUES (?)", (data["address"], ))
            conn.commit()
        response["status"] = "ok"
        response["message"] = "Registration successful"
        code = 201
    print()
    return Response(json.dumps(response), status=code, mimetype="application/json")

@app.route("/users", methods=["GET"])
def get_users():
    conn = sqlite3.connect(database)
    cursor = conn.cursor()
    res = cursor.execute("SELECT * FROM nodes").fetchall()
    response = []
    for record in res:
        response.append({"address": record[1]})
    return Response(json.dumps(response), status=200, mimetype="application/json")

@app.route("/current", methods=["GET"])
def get_current():
    conn = sqlite3.connect(database)
    cursor = conn.cursor()
    res = cursor.execute("SELECT consensus_order.id, consensus_order.node_id, nodes.address from consensus_order INNER JOIN nodes ON consensus_order.node_id = nodes.id ORDER BY consensus_order.id DESC").fetchone()
    if res:
        response = {"slot": res[0], "address": res[2]}
    else:
        response = {}
    return Response(json.dumps(response), status=200, mimetype="application/json")

@app.route("/slot/<int:slot_id>/", methods=["GET"])
def get_slot(slot_id):
    conn = sqlite3.connect(database)
    cursor = conn.cursor()
    res = cursor.execute("SELECT consensus_order.id, consensus_order.node_id, nodes.address from consensus_order INNER JOIN nodes ON consensus_order.node_id = nodes.id WHERE consensus_order.id = ? ORDER BY consensus_order.id DESC", [slot_id]).fetchone()
    if res:
        response = {"slot": res[0], "address": res[2]}
    else:
        response = {}
    return Response(json.dumps(response), status=200, mimetype="application/json")


@app.route("/history", methods=["GET"])
def get_history():
    conn = sqlite3.connect(database)
    cursor = conn.cursor()
    res = cursor.execute("SELECT consensus_order.id, consensus_order.node_id, nodes.address from consensus_order INNER JOIN nodes ON consensus_order.node_id = nodes.id ORDER BY consensus_order.id DESC").fetchall()
    response = []
    for record in res:
        response.append({"slot": record[0], "address": record[2]})
    return Response(json.dumps(response), status=200, mimetype="application/json")

consensus_routine = ConsensusRoutine()
consensus_routine.start()
app.run()