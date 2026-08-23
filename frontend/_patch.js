const fs = require('fs');
let c = fs.readFileSync('src/pages/Subscription.tsx', 'utf8');

const before = '      // Open Razorpay Standard Checkout\n      openRazorpayCheckout({';
const after = `      // Dev mode: detect mock orders when Razorpay is disabled in backend
      if (checkout.razorpayKey === "rzp_test_placeholder" || checkout.razorpayKey === "placeholder_key") {
        try {
          const apiMod = await import("../services/api");
          const apiClient = apiMod.default;
          await apiClient.post("/payment/test-confirm?orderId=" + checkout.orderId);
          setUpgrading("processing");
          await new Promise(r => setTimeout(r, 1000));
          await loadData();
        } catch {
          setError("Test payment simulation failed");
          setFailedPlan(planCode);
          setUpgrading(null);
        }
        return;
      }

      // Real Razorpay flow
      openRazorpayCheckout({`;

if (c.includes('rzp_test_placeholder')) {
  console.log('Mock detection already present');
  process.exit(0);
}

if (!c.includes(before)) {
  console.error('Marker not found!');
  process.exit(1);
}

c = c.replace(before, after);
fs.writeFileSync('src/pages/Subscription.tsx', c);
console.log('Mock detection inserted');
