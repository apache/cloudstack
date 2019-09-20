import Vue from 'vue'

import { library } from '@fortawesome/fontawesome-svg-core'
import { FontAwesomeIcon } from '@fortawesome/vue-fontawesome'

// import { fab } from '@fortawesome/free-brands-svg-icons'
// import { fas } from '@fortawesome/free-solid-svg-icons'
// import { far } from '@fortawesome/free-regular-svg-icons'

import { faCentos, faUbuntu, faSuse, faRedhat, faFedora, faLinux, faFreebsd, faApple, faWindows, faJava } from '@fortawesome/free-brands-svg-icons'
import { faMicrochip, faMemory, faDatabase, faEthernet, faCompactDisc } from '@fortawesome/free-solid-svg-icons'

library.add(faCentos, faUbuntu, faSuse, faRedhat, faFedora, faLinux, faFreebsd, faApple, faWindows, faJava)
library.add(faMicrochip, faMemory, faDatabase, faEthernet, faCompactDisc)

Vue.component('font-awesome-icon', FontAwesomeIcon)
