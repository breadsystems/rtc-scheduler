function displayLang(lang) {
  if (!lang) return;
  Array.from(document.querySelectorAll("[data-lang]"))
    .forEach(function(el){
      el.style.display = el.dataset.lang === lang ? "initial" : "none";
    })
}
// Display the current language according to the user's preferences
displayLang(navigator.language.slice(0,2)||"en")
document.getElementById('select-language')
  .addEventListener('change', function(e) {
    displayLang(e.target.value)
  })