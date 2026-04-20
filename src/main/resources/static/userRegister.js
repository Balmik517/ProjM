function validateRegister() {

    var email = document.getElementById("email").value;
    var password = document.getElementById("password").value;


    // --------------- Email validation ------------------
    var atposition = email.indexOf("@");
    var dotposition = email.lastIndexOf(".");
    if (atposition < 1 || dotposition < atposition + 2 || dotposition + 2 >= email.length) {
        alert("Please enter a valid e-mail address \n atpostion:" + atposition + "\n dotposition:" + dotposition);
        return false;
    }



    // ------------------------- Password VAlidation -----------------
    //check empty password field
    if (password == "") {
        document.getElementById("message").innerHTML =
            "**Fill the password please!";
        return false;
    }

    //minimum password length validation
    if (password.length < 8) {
        document.getElementById("message").innerHTML =
            "**Password length must be atleast 8 characters";
        return false;
    }

    //maximum length of password validation
    if (password.length > 15) {
        document.getElementById("message").innerHTML =
            "**Password length must not exceed 15 characters";
        return false;
    } 
    
    
    
    
}