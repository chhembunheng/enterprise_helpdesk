const $ = id => document.getElementById(id);
let authUrl, accessToken, account;
const message = (text, error = false) => {
  $('message').textContent = text;
  $('message').classList.toggle('error', error);
  $('message').hidden = !text;
};
function showMode(register) {
  $('login-form').hidden = register; $('register-form').hidden = !register;
  $('login-tab').classList.toggle('selected', !register);
  $('register-tab').classList.toggle('selected', register);
  $('login-tab').setAttribute('aria-pressed', !register);
  $('register-tab').setAttribute('aria-pressed', register);
}
function logout() {
  accessToken = null; account = null;
  $('auth-panel').hidden = false; $('ticket-panel').hidden = true;
  $('tickets').replaceChildren(); $('login-form').reset(); showMode(false);
}
async function request(url, options = {}) {
  const response = await fetch(url, options);
  const data = await response.json().catch(() => null);
  if (!response.ok) {
    if (response.status === 401 && accessToken) { logout(); throw Error('Your session expired. Please sign in again.'); }
    const errors = {400:'Please check your input and try again.',401:'Incorrect username or password.',409:'That username or email is already registered.'};
    throw Error(errors[response.status] || 'The request could not be completed. Please try again.');
  }
  return data;
}
function json(body, authenticated = false) {
  const headers = {'Content-Type':'application/json'};
  if (authenticated) headers.Authorization = 'Bearer ' + accessToken;
  return {method:'POST',headers,body:JSON.stringify(body)};
}
async function busy(form, action) {
  const button = form.querySelector('button[type="submit"]'); button.disabled = true; message('');
  try { await action(); } catch (error) { message(error.message || 'Cannot connect to the service.', true); }
  finally { button.disabled = false; }
}
async function loadTickets() {
  const tickets = await request('/tickets', {headers:{Authorization:'Bearer ' + accessToken}});
  const container = $('tickets'); container.replaceChildren();
  if (!tickets.length) { const p = document.createElement('p'); p.className = 'muted'; p.textContent = 'No tickets yet. Your first request starts above.'; container.append(p); }
  for (const ticket of tickets) {
    const article = document.createElement('article'); article.className = 'ticket';
    const title = document.createElement('strong'); title.textContent = ticket.title;
    const detail = document.createElement('p'); detail.textContent = ticket.description || '';
    const status = document.createElement('span'); status.className = 'badge'; status.textContent = ticket.status;
    const id = document.createElement('small'); id.textContent = 'Ticket ' + ticket.id;
    article.append(title, detail, status, id); container.append(article);
  }
}
$('login-tab').onclick = () => showMode(false);
$('register-tab').onclick = () => showMode(true);
$('logout').onclick = () => { logout(); message('Signed out.'); };
$('refresh').onclick = () => loadTickets().catch(error => message(error.message, true));
$('register-form').onsubmit = event => {
  event.preventDefault(); const form = event.currentTarget;
  busy(form, async () => {
    const body = Object.fromEntries(new FormData(form));
    if (new TextEncoder().encode(body.password).length > 72) throw Error('Password must be at most 72 UTF-8 bytes.');
    if (!authUrl) throw Error('Services are still loading. Please try again.');
    const user = await request(authUrl + '/auth/register', json(body));
    form.reset(); showMode(false); $('login-username').value = user.username;
    message('Account created. Sign in with your new username and password.'); $('login-password').focus();
  });
};
$('login-form').onsubmit = event => {
  event.preventDefault(); const form = event.currentTarget;
  busy(form, async () => {
    if (!authUrl) throw Error('Services are still loading. Please try again.');
    const result = await request(authUrl + '/auth/login', json(Object.fromEntries(new FormData(form))));
    accessToken = result.accessToken; account = result.user; form.reset();
    $('account-name').textContent = account.username; $('auth-panel').hidden = true; $('ticket-panel').hidden = false;
    await loadTickets(); message('You’re signed in. Tell us how we can help.');
  });
};
$('ticket-form').onsubmit = event => {
  event.preventDefault(); const form = event.currentTarget;
  busy(form, async () => {
    const result = await request('/tickets', json(Object.fromEntries(new FormData(form)), true));
    form.reset(); message('Ticket submitted. Reference: ' + result.id); await loadTickets();
  });
};
request('/client-config').then(config => { authUrl = config.authUrl.replace(/\/$/, ''); })
  .catch(() => message('Cannot load service configuration. Refresh to try again.', true));
