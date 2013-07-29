from flask import Flask, url_for, render_template, request, json, abort, send_from_directory
from api import apicall

app = Flask(__name__)

def get_args(multidict):
    """Default type of request.args or request.json is multidict. Converts it to dict so that can be passed to make_request"""
    data = {}
    for key in multidict.keys():
        data[key] = multidict.get(key)
    return data

@app.route('/api/<command>', methods=['GET'])
def rawapi(command):
    if request.method == 'GET':
        return apicall(command, get_args(request.args))

@app.route('/')
def index():
    return send_from_directory("templates", "index.html")

if __name__ == '__main__':
    app.run(debug=True)
