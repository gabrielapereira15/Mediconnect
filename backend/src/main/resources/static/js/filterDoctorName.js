function filterDoctorOptions() {
    const filterInput = document.getElementById('doctorNameFilter').value.toLowerCase();
    const selectElement = document.getElementById('doctorId');
    const options = selectElement.options;
    let firstVisibleOption = null;

    for (let i = 0; i < options.length; i++) {
        const option = options[i];
        const text = option.textContent || option.innerText;

        // Show/hide options based on input
        if (text.toLowerCase().includes(filterInput)) {
            option.style.display = '';
            // Track the first visible option for potential selection
            if (!firstVisibleOption) {
                firstVisibleOption = option;
            }
        } else {
            option.style.display = 'none';
        }
    }

    // Set up event listener for "Enter" keypress
    document.getElementById('doctorNameFilter').addEventListener('keydown', function (event) {
        if (event.key === 'Enter' && firstVisibleOption) {
            event.preventDefault(); // Prevent form submission
            selectElement.value = firstVisibleOption.value; // Change the select value
        }
    });
}
