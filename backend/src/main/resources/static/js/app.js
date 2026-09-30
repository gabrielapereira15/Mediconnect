/*
 * The back office's only script of its own. Everything else is htmx and
 * plain forms, so the pages work as ordinary links and posts too.
 */

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
