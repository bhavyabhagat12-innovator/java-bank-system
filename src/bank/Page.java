package bank;

public class Page {
    public static final String HTML = """
<!DOCTYPE html>
<html lang="en">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>Java Bank</title>
<style>
  body { font-family: Arial, sans-serif; background:#eef2f7; margin:0; padding:20px; }
  .box { max-width:520px; margin:auto; background:#fff; padding:22px; border-radius:12px; box-shadow:0 2px 10px rgba(0,0,0,.1); }
  h1 { text-align:center; color:#1a3d7c; margin-top:0; }
  h3 { color:#1a3d7c; }
  input, select { width:100%; padding:10px; margin:5px 0 10px; border:1px solid #ccc; border-radius:6px; box-sizing:border-box; }
  button { padding:10px 14px; margin:3px 2px; background:#1a3d7c; color:#fff; border:none; border-radius:6px; cursor:pointer; }
  button:hover { background:#2c5cb8; }
  button.red { background:#b83232; }
  pre { background:#f5f5f5; padding:12px; border-radius:6px; white-space:pre-wrap; min-height:40px; }
  .ok { color:#116611; } .err { color:#b00020; }
  .hidden { display:none; }
</style>
</head>
<body>
<div class="box">
  <h1>Java Bank</h1>

  <div id="home">
    <h3>Login</h3>
    <input id="lacc" placeholder="Account number">
    <input id="lpin" type="password" placeholder="4-digit PIN">
    <button onclick="login()">Login</button>

    <h3>Create Account</h3>
    <input id="cname" placeholder="Full name">
    <select id="ctype"><option value="1">Savings (min balance 500)</option><option value="2">Current</option></select>
    <input id="cpin" type="password" placeholder="Set 4-digit PIN">
    <input id="camt" placeholder="Opening balance">
    <button onclick="createAcc()">Create Account</button>
  </div>

  <div id="dash" class="hidden">
    <h3 id="welcome"></h3>
    <input id="amount" placeholder="Amount">
    <input id="to" placeholder="Destination account (for transfer)">
    <input id="newpin" placeholder="New PIN (for change PIN)">
    <button onclick="act('deposit')">Deposit</button>
    <button onclick="act('withdraw')">Withdraw</button>
    <button onclick="act('balance')">Balance</button>
    <button onclick="act('info')">Account Info</button>
    <button onclick="act('statement')">Mini Statement</button>
    <button onclick="act('transfer')">Transfer</button>
    <button onclick="act('interest')">Add Interest</button>
    <button onclick="act('changepin')">Change PIN</button>
    <button class="red" onclick="logout()">Logout</button>
  </div>

  <pre id="out"></pre>
</div>

<script>
let acc = "", pin = "";
const $ = id => document.getElementById(id);

async function call(path, data) {
  const r = await fetch(path, {
    method: "POST",
    headers: {"Content-Type": "application/x-www-form-urlencoded"},
    body: new URLSearchParams(data)
  });
  return r.json();
}

function show(res) {
  $("out").className = res.ok ? "ok" : "err";
  $("out").textContent = res.message;
}

async function createAcc() {
  show(await call("/api/create", {name: $("cname").value, type: $("ctype").value, pin: $("cpin").value, amount: $("camt").value}));
}

async function login() {
  const res = await call("/api/action", {action: "login", acc: $("lacc").value, pin: $("lpin").value});
  show(res);
  if (res.ok) {
    acc = $("lacc").value; pin = $("lpin").value;
    $("home").classList.add("hidden");
    $("dash").classList.remove("hidden");
    $("welcome").textContent = res.message;
  }
}

async function act(action) {
  show(await call("/api/action", {action: action, acc: acc, pin: pin, amount: $("amount").value, to: $("to").value, newpin: $("newpin").value}));
  if (action === "changepin" && $("out").className === "ok") pin = $("newpin").value;
}

function logout() {
  acc = ""; pin = "";
  $("dash").classList.add("hidden");
  $("home").classList.remove("hidden");
  $("out").textContent = "Logged out.";
  $("out").className = "ok";
}
</script>
</body>
</html>
""";
}
