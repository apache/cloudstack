<template class="trend">
  <div class="trend-wrapper chart-trend">
    {{ term }}
    <span class="trend-rate">{{ rate }}%</span>
    <span class="trend-icon " :class="[trend]"><a-icon :type="'caret-' + trend"/></span>
  </div>
</template>

<script>
export default {
  name: 'Trend',
  props: {
    term: {
      type: String,
      default: '',
      required: true
    },
    percentage: {
      type: Number,
      default: null
    },
    type: {
      type: Boolean,
      default: null
    },
    target: {
      type: Number,
      default: 0
    },
    value: {
      type: Number,
      default: 0
    },
    fixed: {
      type: Number,
      default: 2
    }
  },
  data () {
    return {
      trend: this.type && 'up' || 'down',
      rate: this.percentage
    }
  },
  created () {
    const type = this.type === null ? this.value >= this.target : this.type
    this.trend = type ? 'up' : 'down'
    this.rate = (this.percentage === null ? Math.abs(this.value - this.target) * 100 / this.target : this.percentage).toFixed(this.fixed)
  }
}
</script>

<style lang="less" scoped>
.trend {
  &-wrapper {
    display: inline-block;
    font-size: 14px;
    line-height: 22px;
  }

  &-icon {
    font-size: 12px;

    &.up,
    &.down {
      margin-left: 4px;
      position: relative;

      i {
        font-size: 12px;
        transform: scale(.83);
      }
    }

    &.up {
      color: #f5222d;
      top: 1px;
    }

    &.down {
      color: #52c41a;
      top: -1px;
    }
  }
}
</style>
