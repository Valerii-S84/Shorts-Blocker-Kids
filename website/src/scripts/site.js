(() => {
  const widgets = document.querySelectorAll("[data-metrics-widget]");

  widgets.forEach((widget) => {
    const endpoint = widget.getAttribute("data-endpoint");
    if (!endpoint) {
      return;
    }

    const statusNode = widget.querySelector("[data-metrics-status]");
    const detailNode = widget.querySelector("[data-metrics-detail]");
    loadMetrics(endpoint, statusNode, detailNode);
  });

  async function loadMetrics(endpoint, statusNode, detailNode) {
    try {
      const response = await fetch(endpoint, {
        headers: { Accept: "application/json" },
        cache: "no-store",
      });
      if (!response.ok) {
        return;
      }
      const payload = await response.json();
      renderVerifiedMetrics(payload, statusNode, detailNode);
    } catch {
      // Keep the static neutral state when metrics are unavailable.
    }
  }

  function renderVerifiedMetrics(payload, statusNode, detailNode) {
    if (!isVerifiedMetricsPayload(payload)) {
      return;
    }

    const families = new Intl.NumberFormat("en").format(payload.privateTestingFamilies);
    const blockedEvents = new Intl.NumberFormat("en").format(payload.blockedEvents);
    statusNode.textContent = `${families} private testing families`;
    detailNode.textContent = `${blockedEvents} verified blocked events reported by the metrics endpoint.`;
  }

  function isVerifiedMetricsPayload(payload) {
    return Boolean(
      payload &&
        payload.verified === true &&
        Number.isSafeInteger(payload.privateTestingFamilies) &&
        payload.privateTestingFamilies >= 0 &&
        Number.isSafeInteger(payload.blockedEvents) &&
        payload.blockedEvents >= 0 &&
        typeof payload.generatedAt === "string" &&
        payload.generatedAt.length > 0,
    );
  }
})();
