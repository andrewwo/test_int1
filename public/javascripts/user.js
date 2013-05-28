function checkPassword(){
    var password = $("#password").val();
    var confirmPassword = $("#confirmPassword").val();

    if(password == confirmPassword)
        return true;

    $("#alerts").append('<div class="alert alert-error"><a class="close" data-dismiss="alert">×</a>' + 'Password does not match' + '</div>');
    return false;
}

function confirmDelete() {
	return confirm("Are you sure you want to delete this user?");
}
