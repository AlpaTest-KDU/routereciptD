function confirmLogout (formId) {
    if (confirm("정말 로그아웃 하시겠습니까?")) {
        const form = document.getElementById(formId);
        
        if (form) {
            form.submit();
        } else {
            console.error("폼을 찾을 수 없습니다. : " + formId);
        }
    }
}