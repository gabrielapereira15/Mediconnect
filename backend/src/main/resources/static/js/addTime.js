// JavaScript function to add a new time input
function addTimeInput() {
    const container = document.getElementById('times-container');
    const newInput = document.createElement('div');
    newInput.innerHTML = `
        <div th:replace="~{fragments/forms::inputRow(object='scheduleTime', field='time', inputClass='js-timepicker', required=true)}" />
        <button type="button" onclick="removeTimeInput(this)" class="btn btn-danger btn-sm">Remove</button>
    `;
    container.appendChild(newInput);
}

function removeTimeInput(button) {
    const parentDiv = button.parentElement;
    parentDiv.remove();
}