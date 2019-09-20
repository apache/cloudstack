<template>
  <a-modal
    :title="currentAction.label"
    :visible="showForm"
    :closable="true"
    :confirmLoading="currentAction.loading"
    style="top: 20px;"
    @ok="handleSubmit"
    @cancel="close"
    centered
  >
    <a-spin :spinning="currentAction.loading">
      <a-form
        :form="form"
        @submit="handleSubmit"
        layout="vertical" >
        <a-form-item
          v-for="(field, fieldIndex) in currentAction.params"
          :key="fieldIndex"
          :label="$t(field.name)"
          :v-bind="field.name"
          v-if="field.name !== 'id'"
        >
          <span v-if="field.type==='boolean'">
            <a-switch
              v-decorator="[field.name, {
                rules: [{ required: field.required, message: 'Please provide input' }]
              }]"
              :placeholder="field.description"
            />
          </span>
          <span v-else-if="field.type==='uuid' || field.name==='account'">
            <a-select
              :loading="field.loading"
              v-decorator="[field.name, {
                rules: [{ required: field.required, message: 'Please select option' }]
              }]"
              :placeholder="field.description"

            >
              <a-select-option v-for="(opt, optIndex) in field.opts" :key="optIndex">
                {{ opt.name }}
              </a-select-option>
            </a-select>
          </span>
          <span v-else-if="field.type==='long'">
            <a-input-number
              v-decorator="[field.name, {
                rules: [{ required: field.required, message: 'Please enter a number' }]
              }]"
              :placeholder="field.description"
            />
          </span>
          <span v-else>
            <a-input
              v-decorator="[field.name, {
                rules: [{ required: field.required, message: 'Please enter input' }]
              }]"
              :placeholder="field.description"
            />
          </span>
        </a-form-item>
      </a-form>
    </a-spin>
  </a-modal>
</template>

<script>

import ChartCard from '@/components/chart/ChartCard'

export default {
  name: 'FormView',
  components: {
    ChartCard
  },
  props: {
    currentAction: {
      type: Object,
      required: true
    },
    showForm: {
      type: Boolean,
      default: false
    },
    handleSubmit: {
      type: Function,
      default: () => {}
    }
  },
  beforeCreate () {
    this.form = this.$form.createForm(this)
  },
  methods: {
    close () {
      this.currentAction.loading = false
      this.showForm = false
    }
  }
}
</script>

<style scoped>
</style>
