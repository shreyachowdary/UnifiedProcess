const API_BASE = '';

const views = document.querySelectorAll('.view');
const navBtns = document.querySelectorAll('.nav-btn');

navBtns.forEach(btn => {
  btn.addEventListener('click', () => {
    const viewId = btn.dataset.view;
    views.forEach(v => v.classList.remove('active'));
    navBtns.forEach(b => b.classList.remove('active'));
    document.getElementById(viewId).classList.add('active');
    btn.classList.add('active');
    if (viewId === 'dashboard') loadMetrics();
  });
});

async function checkStatus() {
  const el = document.getElementById('status-indicator');
  try {
    const res = await fetch(`${API_BASE}/actuator/health`);
    el.textContent = 'Connected';
    el.className = 'connected';
  } catch (e) {
    el.textContent = 'API not running';
    el.className = 'error';
  }
}

async function loadMetrics() {
  await checkStatus();
  try {
    const res = await fetch(`${API_BASE}/api/v1/metrics/summary`);
    const data = await res.json();
    document.getElementById('metric-total').textContent = data.total;
    document.getElementById('metric-success').textContent = data.successRate.toFixed(1) + '%';
    document.getElementById('metric-latency').textContent = data.avgLatencyMs.toFixed(0) + ' ms';
    document.getElementById('metric-hour').textContent = data.lastHourVolume;
  } catch (e) {
    document.getElementById('metric-total').textContent = '—';
    document.getElementById('metric-success').textContent = '—';
    document.getElementById('metric-latency').textContent = '—';
    document.getElementById('metric-hour').textContent = '—';
  }
}

document.getElementById('refresh-metrics').addEventListener('click', loadMetrics);

document.getElementById('send-form').addEventListener('submit', async (e) => {
  e.preventDefault();
  const resultEl = document.getElementById('send-result');
  resultEl.classList.remove('show', 'success', 'error');

  let variables = {};
  const varsInput = document.getElementById('variables').value.trim();
  if (varsInput) {
    try {
      variables = JSON.parse(varsInput);
    } catch (err) {
      resultEl.textContent = 'Invalid variables JSON';
      resultEl.classList.add('show', 'error');
      return;
    }
  }

  const payload = {
    clientId: document.getElementById('clientId').value,
    channel: document.getElementById('channel').value,
    to: document.getElementById('to').value,
    templateId: document.getElementById('templateId').value,
    variables,
    idempotencyKey: document.getElementById('idempotencyKey').value || null
  };

  if (!payload.idempotencyKey) delete payload.idempotencyKey;

  try {
    const res = await fetch(`${API_BASE}/api/v1/notifications`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(payload)
    });
    const data = await res.json();

    if (res.ok) {
      resultEl.innerHTML = `Notification queued! Request ID: <strong>${data.requestId}</strong><br><small>Copy this ID to check status.</small>`;
      resultEl.classList.add('show', 'success');
      document.getElementById('requestId').value = data.requestId;
    } else {
      resultEl.textContent = data.error || 'Request failed';
      resultEl.classList.add('show', 'error');
    }
  } catch (err) {
    resultEl.textContent = 'Request failed. Is the API running?';
    resultEl.classList.add('show', 'error');
  }
});

document.getElementById('check-status').addEventListener('click', async () => {
  const requestId = document.getElementById('requestId').value.trim();
  const resultEl = document.getElementById('status-result');
  resultEl.classList.remove('show', 'success', 'error');

  if (!requestId) {
    resultEl.textContent = 'Enter a request ID';
    resultEl.classList.add('show', 'error');
    return;
  }

  try {
    const res = await fetch(`${API_BASE}/api/v1/notifications/${requestId}`);
    const data = await res.json();

    if (res.ok) {
      resultEl.innerHTML = `
        <div class="status-detail">
          Status: <strong>${data.status}</strong><br>
          Channel: ${data.channel} | To: ${data.to}<br>
          Created: ${new Date(data.createdAt).toLocaleString()}<br>
          ${data.deliveryLogs?.length ? 'Delivery logs: ' + JSON.stringify(data.deliveryLogs, null, 2) : ''}
        </div>
      `;
      resultEl.classList.add('show', 'success');
    } else {
      resultEl.textContent = data.error || 'Not found';
      resultEl.classList.add('show', 'error');
    }
  } catch (err) {
    resultEl.textContent = 'Request failed. Is the API running?';
    resultEl.classList.add('show', 'error');
  }
});

document.getElementById('channel').addEventListener('change', (e) => {
  const templateSelect = document.getElementById('templateId');
  const templates = {
    EMAIL: [
      { id: 'welcome-email', label: 'welcome-email' }
    ],
    SMS: [
      { id: 'welcome-sms', label: 'welcome-sms' },
      { id: 'otp-sms', label: 'otp-sms' }
    ]
  };
  const opts = templates[e.target.value] || templates.EMAIL;
  templateSelect.innerHTML = opts.map(t => `<option value="${t.id}">${t.label}</option>`).join('');
  document.getElementById('variables').value = templateSelect.value === 'otp-sms' ? '{"code":"1234"}' : '{"name":"John"}';
});

loadMetrics();
