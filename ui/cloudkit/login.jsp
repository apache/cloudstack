<% long now = System.currentTimeMillis(); %>

<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
    <meta http-equiv='cache-control' content='no-cache'>
    <meta http-equiv='expires' content='0'>
    <meta http-equiv='pragma' content='no-cache'>
	<link rel="stylesheet" href="css/main.css" type="text/css" />

	<!-- Common libraries -->
	<script type="text/javascript" src="scripts/json2.js"></script>
    <script type="text/javascript" src="../scripts/jquery.min.js"></script>
    <script type="text/javascript" src="../scripts/jquery-ui.custom.min.js"></script>
    <script type="text/javascript" src="../scripts/date.js"></script>
    <script type="text/javascript" src="../scripts/jquery.cookies.js"></script>
    <script type="text/javascript" src="../scripts/jquery.timers.js"></script>
    <script type="text/javascript" src="../scripts/jquery.md5.js"></script>

    <!-- cloud.com scripts -->
	<script type="text/javascript" src="scripts/cloudkit.js?t=<%=now%>"></script>
	<script type="text/javascript" src="scripts/cloudkit.login.js?t=<%=now%>"></script>
	
	<!-- Favicon -->
	<link rel="shortcut icon" href="favicon.ico" type="image/x-icon" />

    <title>myCloud - Login</title>
</head>

<body>
	<div id="loginmain" style="display:none">
		<div class="login_box">
			<div class="login_box_top">
				<div class="login_logo"></div>
			</div>
			<div class="login_box_mid">
				<h2>Login to your myCloud Account</h2>
				<div id="login_form" class="login_formbox">
					<form>
						<ul>
							<li>
								<label for="username">Username</label>
								<input type="text" class="text" id="login_username"/>
							</li>
							
							<li>
								<label for="password">Password</label>
								<input type="password" class="text" id="login_password"/>
							</li>
						</ul>
						<div class="login_submitbox">
							<div class="login_button" value="" id="login_submit" ></div>
						  
						</div>
					</form>
					<div id="login_error" class="login_errormsgbox" style="display:none;">
						<p>Your username/password does not match our records.</p>
					</div>
				</div>
			</div>
			<div class="login_box_bot"></div>
		</div>
    </div>
</body>
</html>
