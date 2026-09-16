// Темата се поставува ПРЕД React да се вчита, инаку страницата за миг трепка во
// светла тема пред да се пресмета точната. Истата логика е и во ThemeContext.tsx.
// Стои во посебна датотека, а не во index.html, за да не треба CSP да дозволува
// inline скрипти — една дозволена inline скрипта ја отвора вратата за сите.
;(function () {
  try {
    var stored = localStorage.getItem('focuslab_theme')
    var dark =
      stored === 'dark' ||
      (!stored && window.matchMedia('(prefers-color-scheme: dark)').matches)

    if (dark) {
      document.documentElement.classList.add('dark')
    }
  } catch (error) {
    // приватен режим / блокирано складирање — се продолжува со светла тема
  }
})()
