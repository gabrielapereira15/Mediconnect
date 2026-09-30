function initDatepicker() {
  document.querySelectorAll('.js-datepicker, .js-timepicker, .js-datetimepicker').forEach(($item) => {
    const flatpickrConfig = {
      allowInput: true,
      time_24hr: false,
      enableSeconds: false
    };
    if ($item.classList.contains('js-datepicker')) {
      flatpickrConfig.dateFormat = 'Y-m-d';
    } else if ($item.classList.contains('js-timepicker')) {
      flatpickrConfig.enableTime = true;
      flatpickrConfig.noCalendar = true;
      flatpickrConfig.dateFormat = 'H:i K';
    } else { // datetimepicker
      flatpickrConfig.enableTime = true;
      flatpickrConfig.altInput = true;
      flatpickrConfig.altFormat = 'Y-m-d H:i:S';
      flatpickrConfig.dateFormat = 'Y-m-dTH:i:S';
      // workaround label issue
      flatpickrConfig.onReady = function() {
        const id = this.input.id;
        this.input.id = null;
        this.altInput.id = id;
      };
    }
    flatpickr($item, flatpickrConfig);
  });
}
document.addEventListener('htmx:afterSwap', initDatepicker);
initDatepicker();

/*
 * A row that opens the side panel shows it is the open one. The panel is
 * swapped in by htmx, so the page does not reload to move the highlight.
 */
document.addEventListener('htmx:beforeRequest', (event) => {
  const row = event.detail.elt;
  if (!row || !row.matches || !row.matches('tr[data-clickable]')) {
    return;
  }
  row.closest('tbody').querySelectorAll('tr.is-selected').forEach((other) => {
    other.classList.remove('is-selected');
    other.removeAttribute('aria-selected');
  });
  row.classList.add('is-selected');
  row.setAttribute('aria-selected', 'true');
});
