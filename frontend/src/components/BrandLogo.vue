<template>
  <div class="brand">
    <span class="brand-mark">铸</span>
    <div class="brand-text">
      <span class="brand-name">铸语</span>
      <span class="brand-sub">PROMPTFORGE</span>
    </div>
  </div>
</template>

<style scoped>
.brand {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 20px 18px;
}

.brand-mark {
  width: 32px;
  height: 32px;
  border-radius: 8px;
  background: linear-gradient(135deg, #38bdf8 0%, #2563eb 100%);
  box-shadow: 0 3px 10px rgba(37, 99, 235, 0.28);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 16px;
  font-weight: 600;
  flex-shrink: 0;
}

.brand-text {
  display: flex;
  flex-direction: column;
  line-height: 1.3;
}

.brand-name {
  font-size: 15.5px;
  font-weight: 700;
  color: var(--text-1);
  letter-spacing: 0.5px;
}

.brand-sub {
  font-family: var(--font-mono);
  font-size: 9px;
  color: var(--text-3);
  letter-spacing: 3px;
}
</style>
