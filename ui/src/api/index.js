import { axios } from '@/utils/request'

export function api (command, args = {}) {
  args['command'] = command
  args['response'] = 'json'
  return axios.get('/', {
    params: args
  })
}

export function login (arg) {
  const params = new URLSearchParams()
  params.append('command', 'login')
  params.append('username', arg.username)
  params.append('password', arg.password)
  params.append('domain', arg.domain)
  params.append('response', 'json')
  return axios({
    url: '/',
    method: 'post',
    data: params,
    headers: {
      'Content-Type': 'application/x-www-form-urlencoded'
    }
  })
}

export function logout () {
  return api('logout')
}
